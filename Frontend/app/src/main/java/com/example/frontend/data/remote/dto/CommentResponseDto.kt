package com.example.frontend.data.remote.dto

data class CommentResponseDto(
    val id: String,
    val postId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String = "",
    val content: String,
    val createdAt: String,
    val likeCount: Int = 0,
    val parentCommentId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)
