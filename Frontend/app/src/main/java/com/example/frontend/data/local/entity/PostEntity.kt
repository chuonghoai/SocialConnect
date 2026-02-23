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
    val cdnUrl: String
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
            cdnUrl = cdnUrl
        )
    }
}

fun Post.toEntity(): PostEntity {
    return PostEntity(
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
        cdnUrl = cdnUrl
    )
}