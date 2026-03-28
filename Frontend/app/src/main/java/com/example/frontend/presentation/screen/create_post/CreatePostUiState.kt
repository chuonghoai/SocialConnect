package com.example.frontend.presentation.screen.create_post

import android.net.Uri
import com.example.frontend.domain.model.PostVisibility

data class CreatePostUiState(
    val content: String = "",
    val selectedMediaUris: List<Uri> = emptyList(),
    val visibility: String = PostVisibility.PUBLIC,
    val isLoading: Boolean = false,
    val error: String? = null
)
