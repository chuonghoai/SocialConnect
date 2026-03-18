package com.example.frontend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.frontend.domain.model.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val displayName: String,
    val userAvatar: String,
    val content: String,
    val type: String,
    val kind: String,
    val createdAt: String,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val cdnUrl: String,
    val isLiked: Boolean
) {
    fun toDomain(): Post {
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
            isLiked = isLiked
        )
    }
}

fun Post.toEntity(): PostEntity {
    val safeMedia = media.orEmpty() + mediaIds.orEmpty()
    val safeRawUrls = (mediaUrls.orEmpty() + images.orEmpty() + videos.orEmpty())
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val mediaUrls = safeMedia
        .map { it.resolvedUrl().trim() }
        .filter { it.isNotEmpty() }
        .plus(safeRawUrls)
        .distinct()

    val storageCdnUrl = if (mediaUrls.isNotEmpty()) {
        mediaUrls.joinToString("|")
    } else {
        cdnUrl
    }

    val storageKind = when {
        safeMedia.isEmpty() -> kind
        safeMedia.all { it.kind.orEmpty().uppercase().contains("VIDEO") } -> "VIDEO"
        safeMedia.all { it.kind.orEmpty().uppercase().contains("IMAGE") } -> "IMAGE"
        else -> kind
    }

    return PostEntity(
        id = id,
        userId = userId,
        displayName = displayName,
        userAvatar = userAvatar,
        content = content,
        type = type,
        kind = storageKind,
        createdAt = createdAt,
        likeCount = likeCount,
        commentCount = commentCount,
        shareCount = shareCount,
        cdnUrl = storageCdnUrl,
        isLiked = isLiked
    )
}
