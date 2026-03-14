package com.example.frontend.data.remote.dto

data class CreatePostRequest(
    val content: String,
    val visibility: String,
    val mediaId: List<String>? = null
)