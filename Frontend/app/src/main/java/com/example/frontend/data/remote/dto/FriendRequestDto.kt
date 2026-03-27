package com.example.frontend.data.remote.dto

data class FriendRequestUserDto(
    val id: String,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

data class FriendRequestDto(
    val id: String,
    val status: String,
    val user1: FriendRequestUserDto,
    val user2: FriendRequestUserDto
)
