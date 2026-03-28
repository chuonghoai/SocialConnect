package com.example.frontend.data.remote.dto

data class CommentMediaDto(
    val cdnUrl: String = "",
    val kind: String = ""
)

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
    val mediaType: String? = null,
    val media: List<CommentMediaDto> = emptyList(),
    val mediaUrls: List<String> = emptyList()
)
