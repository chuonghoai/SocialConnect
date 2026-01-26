package com.example.frontend.data.remote.dto

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val mailOtp: String
)