package com.example.frontend.data.remote.dto

data class FriendListResponseDto(
    val total: Int,
    val friends: List<FriendListItem>
)
