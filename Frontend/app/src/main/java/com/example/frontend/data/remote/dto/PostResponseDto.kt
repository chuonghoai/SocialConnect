package com.example.frontend.data.remote.dto

data class PostResponseDto (
    val id: String,
    val userId: String,
    val displayName: String,
    val userAvatar: String = "",
    val content: String,
    val type: String = "ORIGINAL",
    val kind: String = "TEXT",
    val createdAt: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val cdnUrl: String
)