package com.example.frontend.domain.model

data class AdminUserItem(
    val id: String,
    val displayName: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val role: String,
    val isBlock: Boolean
)
