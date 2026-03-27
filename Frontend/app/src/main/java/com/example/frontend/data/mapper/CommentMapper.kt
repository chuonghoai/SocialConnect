package com.example.frontend.data.mapper

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.model.PostMedia

fun CommentResponseDto.toDomain(): Comment = Comment(
    id = id,
    userId = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    content = content,
    createdAt = createdAt,
    likeCount = likeCount,
    parentCommentId = parentCommentId,
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    media = resolveCommentMedia()
)

private fun CommentResponseDto.resolveCommentMedia(): List<PostMedia> {
    val explicitMedia = media.mapNotNull { mediaItem ->
        val url = mediaItem.cdnUrl.trim()
        if (url.isBlank()) null else PostMedia(cdnUrl = url, kind = mediaItem.kind)
    }.distinctBy { mediaItem -> mediaItem.resolvedUrl().trim() }
    if (explicitMedia.isNotEmpty()) return explicitMedia

    val fallbackUrls = buildList {
        add(mediaUrl?.trim().orEmpty())
        addAll(mediaUrls.map { it.trim() })
    }
        .filter { it.isNotBlank() }
        .distinct()
    if (fallbackUrls.isEmpty()) return emptyList()

    return fallbackUrls.map { url ->
        PostMedia(cdnUrl = url, kind = mediaType ?: "")
    }
}
