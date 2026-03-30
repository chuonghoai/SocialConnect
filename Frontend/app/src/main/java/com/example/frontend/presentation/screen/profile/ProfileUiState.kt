package com.example.frontend.presentation.screen.profile

import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val selectedTabIndex: Int = 0,
        val posts: List<Post> = emptyList(),
        val savedPosts: List<Post> = emptyList(),
        val isPostsLoading: Boolean = false,
        val isSavedPostsLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val postsError: String? = null,
        val savedPostsError: String? = null
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}
