package com.example.frontend.domain.model

data class User(
    val id: String,
    val displayName: String,
    val username: String,
    val email: String,
    val phone: String,
    val role: String,
    val isOnline: Boolean,
    val postCount: Long,
    val friendCount: Long,
    val caption: String?,
    val avatarUrl: String?,
    val myPosts: List<Post> = emptyList()
)