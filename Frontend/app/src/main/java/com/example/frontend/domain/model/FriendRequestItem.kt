package com.example.frontend.domain.model

data class FriendRequestItem(
    val requestId: String,
    val fromUserId: String,
    val fromDisplayName: String,
    val fromUsername: String,
    val fromAvatarUrl: String?
)
