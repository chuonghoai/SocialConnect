package com.example.frontend.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MeResponseDto(
    val displayName: String,
    val username: String,
    val avatarUrl: String? = null
)