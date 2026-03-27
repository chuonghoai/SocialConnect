package com.example.frontend.data.remote.dto

data class CreateCommentRequest(
    val content: String,
    val parentCommentId: String? = null,
    val mediaId: String? = null,
    val mediaIds: List<String>? = null
)
