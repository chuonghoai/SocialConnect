package com.example.frontend.presentation.screen.video

import com.example.frontend.domain.model.Post

sealed class VideoUiState {
    data object Loading : VideoUiState()
    data class Success(
        val posts: List<Post>,
        val isLoadingMore: Boolean = false
    ) : VideoUiState()
    data class Error(val message: String) : VideoUiState()
}