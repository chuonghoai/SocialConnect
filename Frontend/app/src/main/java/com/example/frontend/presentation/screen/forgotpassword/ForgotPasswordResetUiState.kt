package com.example.frontend.presentation.screen.forgotpassword

data class ForgotPasswordResetUiState(
    val email: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
