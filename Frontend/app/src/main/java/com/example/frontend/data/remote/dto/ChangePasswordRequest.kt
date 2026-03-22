package com.example.frontend.data.remote.dto

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
