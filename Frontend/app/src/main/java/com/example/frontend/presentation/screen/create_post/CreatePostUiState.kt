package com.example.frontend.presentation.screen.create_post

import android.net.Uri

data class CreatePostUiState(
    val content: String = "",
    val selectedMediaUris: List<Uri> = emptyList(),
    val visibility: String = "Công khai",
    val isLoading: Boolean = false,
    val error: String? = null
)