package com.example.frontend.data.remote.dto

data class UpdateProfileRequest(
    val displayName: String,
    val dob: String,
    val email: String,
    val avatar: String?
)
