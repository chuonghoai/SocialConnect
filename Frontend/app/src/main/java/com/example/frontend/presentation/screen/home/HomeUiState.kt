package com.example.frontend.presentation.screen.home

import com.example.frontend.domain.model.Post

sealed class HomeUiState {
    data object Loading: HomeUiState()
    data class Success(
        val posts: List<Post>,
        val isLoadingMore: Boolean = false
    ): HomeUiState()
    data class Error(val message: String) : HomeUiState()
}