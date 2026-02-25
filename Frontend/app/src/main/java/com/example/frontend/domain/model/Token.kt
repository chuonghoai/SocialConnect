package com.example.frontend.domain.model

data class Token(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)