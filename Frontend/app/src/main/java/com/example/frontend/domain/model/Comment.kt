package com.example.frontend.domain.model

data class Comment(
    val id: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val content: String,
    val createdAt: String,
    val likeCount: Int
)
