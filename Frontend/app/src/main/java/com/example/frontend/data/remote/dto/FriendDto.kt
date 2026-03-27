package com.example.frontend.data.remote.dto

data class FriendListItem(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)


data class FriendDto(
    val items: List<FriendListItem>
)