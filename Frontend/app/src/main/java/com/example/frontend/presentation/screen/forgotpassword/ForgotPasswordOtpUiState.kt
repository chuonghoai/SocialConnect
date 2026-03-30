package com.example.frontend.presentation.screen.forgotpassword

data class ForgotPasswordOtpUiState(
    val email: String = "",
    val otp: String = "",
    val canResend: Boolean = false,
    val secondsUntilResend: Int = 0,
    val loading: Boolean = false,
    val error: String? = null
)
