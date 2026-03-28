package com.example.frontend.presentation.screen.create_post

import android.net.Uri

enum class PostComposerMode {
    CREATE,
    EDIT
}

data class CreatePostUiState(
    val mode: PostComposerMode = PostComposerMode.CREATE,
    val editingPostId: String? = null,
    val isInitialized: Boolean = false,
    val content: String = "",
    val selectedMediaUris: List<Uri> = emptyList(),
    val visibility: String = "Công khai",
    val isLoading: Boolean = false,
    val error: String? = null
)
