package com.example.frontend.data.mapper

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
    val createdAt = stringValue("createdAt", "created_at", "timestamp", "date")
    val likeCount = intValue("likeCount", "likes", "totalLikes")
    val commentCount = intValue("commentCount", "comments", "totalComments")
    val shareCount = intValue("shareCount", "shares", "totalShares")
    val isLiked = boolValue("isLiked", "liked")

    val parsedMedia = extractMediaFromRoot()
    val fallbackCdn = stringValue("cdnUrl", "cdn_url", "url", "imageUrl", "videoUrl")
    val cdnUrl = parsedMedia.firstOrNull()?.cdnUrl ?: fallbackCdn

    return Post(
        id = id,
        userId = userId,
        displayName = displayName,
        userAvatar = userAvatar,
        content = content,
        type = type,
        kind = kind,
        createdAt = createdAt,
        likeCount = likeCount,
        commentCount = commentCount,
        shareCount = shareCount,
        cdnUrl = cdnUrl,
        isLiked = isLiked,
        media = parsedMedia,
        mediaIds = emptyList(),
        mediaUrls = emptyList(),
        images = emptyList(),
        videos = emptyList()
    )
}

private fun Map<String, Any?>.extractMediaFromRoot(): List<PostMedia> {
    val results = linkedMapOf<String, String?>()

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
        collectMediaCandidates(value, defaultKind, results)
    }

    // 2) Fallback for simple top-level URL keys.
    for (key in MEDIA_URL_KEYS) {
        val value = this.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
        collectMediaCandidates(value, null, results)
    }

    return results.map { (url, kind) ->
        PostMedia(cdnUrl = url, kind = kind ?: inferKind(url))
    }
}

private fun collectMediaCandidates(
    node: Any?,
    inheritedKind: String?,
    results: MutableMap<String, String?>
) {
    when (node) {
        null -> return

        is String -> {
            val urls = parseUrls(node)
            urls.forEach { url ->
                if (!results.containsKey(url)) {
                    results[url] = inheritedKind ?: inferKind(url)
                }
            }
        }

        is Number, is Boolean -> return

        is List<*> -> {
            node.forEach { collectMediaCandidates(it, inheritedKind, results) }
        }

        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as Map<String, Any?>
            val localKind = map.stringValue("kind", "type", "mediaType", "resource_type")
                .ifBlank { inheritedKind.orEmpty() }
                .ifBlank { null }

            // Direct URL-like fields inside this object.
            for (key in MEDIA_URL_KEYS) {
                val direct = map.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
                collectMediaCandidates(direct, localKind ?: inheritedKind, results)
            }

            // Continue scanning nested media-ish branches.
            map.forEach { (k, v) ->
                val normalizedKey = k.lowercase()
                if (MEDIA_CONTAINER_EXCLUDES.any { normalizedKey.contains(it) }) return@forEach
                if (MEDIA_CONTAINER_HINTS.any { normalizedKey.contains(it) } || MEDIA_URL_KEYS.contains(normalizedKey)) {
                    collectMediaCandidates(v, localKind ?: inheritedKind, results)
                }
            }
        }

        else -> {
            val text = node.toString()
            val urls = parseUrls(text)
            urls.forEach { url ->
                if (!results.containsKey(url)) {
                    results[url] = inheritedKind ?: inferKind(url)
                }
            }
        }
    }
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
