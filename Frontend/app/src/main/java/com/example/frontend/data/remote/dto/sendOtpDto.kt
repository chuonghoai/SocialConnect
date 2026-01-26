package com.example.frontend.data.remote.dto

data class sendOtpRequestDto(
    val email: String,
    val type: String
)

data class sendOtpResponseDto(
    val message: String
)