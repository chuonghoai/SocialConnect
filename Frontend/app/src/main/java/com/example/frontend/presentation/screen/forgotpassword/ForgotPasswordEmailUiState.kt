package com.example.frontend.presentation.screen.forgotpassword

data class ForgotPasswordEmailUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
