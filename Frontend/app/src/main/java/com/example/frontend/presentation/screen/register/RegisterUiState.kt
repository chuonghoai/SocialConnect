package com.example.frontend.presentation.screen.register

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otp: String = "",
    val loading: Boolean = false,
    val error: String? = null
)