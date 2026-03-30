package com.example.frontend.domain.model

data class Comment(
    val id: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val content: String,
    val createdAt: String,
    val likeCount: Int,
    val parentCommentId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val media: List<PostMedia> = emptyList()
)
