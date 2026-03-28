package com.example.frontend.data.mapper

import com.example.frontend.domain.model.OriginalPost
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.PostMedia

private val MEDIA_URL_KEYS = setOf(
    "cdnurl", "cdn_url", "url", "secure_url", "secureurl", "link", "path",
    "mediaurl", "media_url", "imageurl", "videourl"
)

private val MEDIA_CONTAINER_HINTS = listOf(
    "media", "image", "video", "attachment", "file", "asset", "gallery"
)

private val MEDIA_CONTAINER_EXCLUDES = listOf(
    "avatar", "profile", "thumbnail", "thumb"
)

private val URL_REGEX = Regex("""https?://[^\s"'|,\]\[]+""")

fun Map<String, Any?>.toDomainPost(): Post {
    val id = stringValue("_id", "id").ifBlank { "unknown_${hashCode()}" }

    val userObj = mapValue("user", "author", "owner")
    val userId = stringValue("userId", "user_id")
        .ifBlank { userObj.stringValue("_id", "id", "userId") }
    val displayName = stringValue("displayName", "username", "fullName", "name")
        .ifBlank { userObj.stringValue("displayName", "username", "fullName", "name") }
    val userAvatar = stringValue("userAvatar", "avatarUrl", "avatar", "profileImage")
        .ifBlank {
            userObj.stringValue("userAvatar", "avatarUrl", "avatar", "profileImage", "photoUrl")
        }

    val content = stringValue("content", "caption", "text", "description")
    val type = stringValue("type", "postType").ifBlank { "POST" }
    val kind = stringValue("kind", "mediaType", "resource_type")
    val visibility = normalizeVisibility(stringValue("visibility", "privacy"))
    val createdAt = stringValue("createdAt", "created_at", "timestamp", "date")
    val likeCount = intValue("likeCount", "likes", "totalLikes")
    val commentCount = intValue("commentCount", "comments", "totalComments")
    val shareCount = intValue("shareCount", "shares", "totalShares")
    val isLiked = boolValue("isLiked", "liked")
    val isHiddenByAdmin = boolValue("isHiddenByAdmin", "hiddenByAdmin", "is_hidden_by_admin")

    val parsedMedia = extractMediaFromRoot()
    val fallbackCdn = stringValue("cdnUrl", "cdn_url", "url", "imageUrl", "videoUrl")
    val cdnUrl = parsedMedia.firstOrNull()?.cdnUrl ?: fallbackCdn
    val parsedOriginalPost = mapValue("originalPost").toOriginalPostOrNull()

    return Post(
        id = id,
        userId = userId,
        displayName = displayName,
        userAvatar = userAvatar,
        content = content,
        visibility = visibility,
        type = type,
        kind = kind,
        createdAt = createdAt,
        likeCount = likeCount,
        commentCount = commentCount,
        shareCount = shareCount,
        cdnUrl = cdnUrl,
        isLiked = isLiked,
        isHiddenByAdmin = isHiddenByAdmin,
        media = parsedMedia,
        originalPost = parsedOriginalPost,
        mediaIds = emptyList(),
        mediaUrls = emptyList(),
        images = emptyList(),
        videos = emptyList()
    )
}

private fun Map<String, Any?>.toOriginalPostOrNull(): OriginalPost? {
    if (isEmpty()) return null

    val id = stringValue("_id", "id")
    if (id.isBlank()) return null

    val userObj = mapValue("user", "author", "owner")
    val userId = stringValue("userId", "user_id")
        .ifBlank { userObj.stringValue("_id", "id", "userId") }
    val displayName = stringValue("displayName", "username", "fullName", "name")
        .ifBlank { userObj.stringValue("displayName", "username", "fullName", "name") }
    val userAvatar = stringValue("userAvatar", "avatarUrl", "avatar", "profileImage")
        .ifBlank {
            userObj.stringValue("userAvatar", "avatarUrl", "avatar", "profileImage", "photoUrl")
        }

    val content = stringValue("content", "caption", "text", "description")
    val createdAt = stringValue("createdAt", "created_at", "timestamp", "date")
    val media = extractMediaFromRoot()
    val fallbackCdn = stringValue("cdnUrl", "cdn_url", "url", "imageUrl", "videoUrl")
    val cdnUrl = media.firstOrNull()?.cdnUrl ?: fallbackCdn
    val firstKind = media.firstOrNull()?.kind
    val kind = stringValue("kind", "mediaType", "resource_type").ifBlank {
        firstKind ?: inferKind(cdnUrl)
    }

    return OriginalPost(
        id = id,
        userId = userId,
        displayName = displayName,
        userAvatar = userAvatar,
        content = content,
        kind = kind,
        cdnUrl = cdnUrl,
        createdAt = createdAt,
        media = media
    )
}

private fun Map<String, Any?>.extractMediaFromRoot(): List<PostMedia> {
    val results = linkedMapOf<String, ParsedMediaMeta>()

    // 1) Scan likely media containers first.
    for ((key, value) in this) {
        val normalizedKey = key.lowercase()
        if (MEDIA_CONTAINER_EXCLUDES.any { normalizedKey.contains(it) }) continue
        if (!MEDIA_CONTAINER_HINTS.any { normalizedKey.contains(it) }) continue

        val defaultKind = when {
            normalizedKey.contains("video") -> "VIDEO"
            normalizedKey.contains("image") -> "IMAGE"
            else -> null
        }
        collectMediaCandidates(value, defaultKind, null, results)
    }

    // 2) Fallback for simple top-level URL keys.
    for (key in MEDIA_URL_KEYS) {
        val value = this.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
        collectMediaCandidates(value, null, null, results)
    }

    return results.map { (url, meta) ->
        PostMedia(
            cdnUrl = url,
            kind = meta.kind ?: inferKind(url),
            publicId = meta.publicId
        )
    }
}

private fun collectMediaCandidates(
    node: Any?,
    inheritedKind: String?,
    inheritedPublicId: String?,
    results: MutableMap<String, ParsedMediaMeta>
) {
    when (node) {
        null -> return

        is String -> {
            val urls = parseUrls(node)
            urls.forEach { url -> upsertMediaResult(results, url, inheritedKind, inheritedPublicId) }
        }

        is Number, is Boolean -> return

        is List<*> -> {
            node.forEach { collectMediaCandidates(it, inheritedKind, inheritedPublicId, results) }
        }

        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as Map<String, Any?>
            val localKind = map.stringValue("kind", "type", "mediaType", "resource_type")
                .ifBlank { inheritedKind.orEmpty() }
                .ifBlank { null }
            val localPublicId = map.stringValue("publicId", "public_id", "mediaPublicId")
                .ifBlank { inheritedPublicId.orEmpty() }
                .ifBlank { null }

            // Direct URL-like fields inside this object.
            for (key in MEDIA_URL_KEYS) {
                val direct = map.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
                collectMediaCandidates(direct, localKind ?: inheritedKind, localPublicId, results)
            }

            // Continue scanning nested media-ish branches.
            map.forEach { (k, v) ->
                val normalizedKey = k.lowercase()
                if (MEDIA_CONTAINER_EXCLUDES.any { normalizedKey.contains(it) }) return@forEach
                if (MEDIA_CONTAINER_HINTS.any { normalizedKey.contains(it) } || MEDIA_URL_KEYS.contains(normalizedKey)) {
                    collectMediaCandidates(v, localKind ?: inheritedKind, localPublicId, results)
                }
            }
        }

        else -> {
            val text = node.toString()
            val urls = parseUrls(text)
            urls.forEach { url -> upsertMediaResult(results, url, inheritedKind, inheritedPublicId) }
        }
    }
}

private data class ParsedMediaMeta(
    val kind: String?,
    val publicId: String?
)

private fun upsertMediaResult(
    results: MutableMap<String, ParsedMediaMeta>,
    url: String,
    incomingKind: String?,
    incomingPublicId: String?
) {
    val current = results[url]
    val mergedKind = current?.kind
        ?: incomingKind
        ?: inferKind(url)
    val mergedPublicId = current?.publicId
        ?: incomingPublicId?.takeIf { it.isNotBlank() }
    results[url] = ParsedMediaMeta(kind = mergedKind, publicId = mergedPublicId)
}

private fun parseUrls(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return emptyList()

    val extracted = URL_REGEX.findAll(trimmed).map { it.value.trim() }.toList()
    if (extracted.isNotEmpty()) return extracted

    return trimmed.split(Regex("[|,;\\n]"))
        .map { it.trim().trim('"', '\'', '[', ']') }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
}

private fun inferKind(url: String): String {
    val normalized = url.lowercase()
    if (normalized.contains("/video/") || normalized.contains("resource_type=video")) {
        return "VIDEO"
    }

    val extension = normalized.substringBefore("?").substringAfterLast('.', "")
    return if (extension in setOf("mp4", "mov", "webm", "m3u8", "mkv", "avi", "3gp", "flv")) {
        "VIDEO"
    } else {
        "IMAGE"
    }
}

private fun normalizeVisibility(raw: String): String {
    val normalized = raw.trim().lowercase()
    return when {
        normalized.isBlank() -> "Công khai"
        normalized.contains("friend") || normalized.contains("ban be") || normalized.contains("bạn bè") -> "Bạn bè"
        normalized.contains("private") || normalized.contains("rieng tu") || normalized.contains("riêng tư") -> "Riêng tư"
        else -> "Công khai"
    }
}

private fun Map<String, Any?>.stringValue(vararg keys: String): String {
    for (key in keys) {
        val value = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
        val text = value as? String ?: value?.toString().orEmpty()
        if (text.isNotBlank() && text != "null") return text
    }
    return ""
}

private fun Map<String, Any?>.intValue(vararg keys: String): Int {
    for (key in keys) {
        val value = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
        when (value) {
            is Number -> return value.toInt()
            is String -> value.toIntOrNull()?.let { return it }
        }
    }
    return 0
}

private fun Map<String, Any?>.boolValue(vararg keys: String): Boolean {
    for (key in keys) {
        val value = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
        when (value) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
            is String -> {
                if (value.equals("true", ignoreCase = true)) return true
                if (value.equals("false", ignoreCase = true)) return false
            }
        }
    }
    return false
}

private fun Map<String, Any?>.mapValue(vararg keys: String): Map<String, Any?> {
    for (key in keys) {
        val value = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: continue
        if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return value as Map<String, Any?>
        }
    }
    return emptyMap()
}
