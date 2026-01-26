package com.example.frontend.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MeResponseDto(
    val id: String,
    val displayName: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val role: String,
    val isOnline: Boolean = true,
    val postCount: Long = 0,
    val friendCount: Long = 0,
    val myPosts: List<PostResponseDto>,
    val caption: String? = null,
    val avatarUrl: String? = null
)