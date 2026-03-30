package com.example.frontend.domain.model

data class FriendRecipient(
    val id: String,
    val displayName: String,
    val username: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)
