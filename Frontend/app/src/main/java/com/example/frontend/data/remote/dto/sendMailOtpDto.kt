package com.example.frontend.data.remote.dto

data class sendOtpRequestDto(
    val email: String,
    val type: String        // Register or forgot password
)

data class sendOtpResponseDto(
    val message: String
)