package com.example.frontend.data.remote.dto

data class UpdatePostRequest(
    val content: String? = null,
    val visibility: String? = null,
    val mediaPublicIds: List<String>? = null,
    val mediaUrls: List<String>? = null
)
