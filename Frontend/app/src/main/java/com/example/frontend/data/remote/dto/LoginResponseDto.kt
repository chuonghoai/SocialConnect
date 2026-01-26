package com.example.frontend.data.remote.dto

data class LoginResponseDto(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)

data class ApiErrorDto(
    val message: String? = null,
    val code: String? = null
)