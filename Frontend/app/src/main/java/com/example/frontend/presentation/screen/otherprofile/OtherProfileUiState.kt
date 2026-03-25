package com.example.frontend.presentation.screen.otherprofile

import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User

sealed class OtherProfileUiState {
    data object Loading : OtherProfileUiState()
    data class Success(
        val user: User,
        val posts: List<Post> = emptyList(),
        val isPostsLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null
    ) : OtherProfileUiState()

    data class Error(val message: String) : OtherProfileUiState()
}
