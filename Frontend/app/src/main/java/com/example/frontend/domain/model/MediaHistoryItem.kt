package com.example.frontend.domain.model

data class MediaHistoryItem(
    val publicId: String,
    val secureUrl: String,
    val type: String,
    val messageId: String,
    val senderId: String,
    val createAt: String
)