package com.example.frontend.domain.model

data class OriginalPost(
    val id: String,
    val userId: String,
    val displayName: String,
    val userAvatar: String,
    val content: String,
    val kind: String,
    val cdnUrl: String,
    val createdAt: String
)

data class Post(
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
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val originalPost: OriginalPost? = null
)
