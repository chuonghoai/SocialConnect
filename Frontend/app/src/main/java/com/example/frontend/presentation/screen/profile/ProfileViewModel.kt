package com.example.frontend.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getMeUseCase: GetMeUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var isFetchingPosts = false
    private var isLastPage = false

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is ProfileUiState.Success) {
                _uiState.value = ProfileUiState.Loading
            }

            when (val userResult = getMeUseCase(isRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    val user = userResult.data

                    val currentState = _uiState.value as? ProfileUiState.Success
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        posts = currentState?.posts ?: emptyList(),
                        isPostsLoading = !isRefresh
                    )

                    isLastPage = false
                    loadUserPosts(user.id, isRefresh)
                }
                is ApiResult.Error -> _uiState.value = ProfileUiState.Error(userResult.message)
            }

            if (isRefresh) _isRefreshing.value = false
        }
    }

    private suspend fun loadUserPosts(userId: String, isRefresh: Boolean) {
        isFetchingPosts = true
        when (val postResult = getUserPostsUseCase(userId, null, isRefresh)) {
            is ApiResult.Success -> {
                val currentState = _uiState.value as? ProfileUiState.Success ?: return
                _uiState.value = currentState.copy(
                    posts = postResult.data,
                    isPostsLoading = false,
                    postsError = null
                )
                if (postResult.data.isEmpty()) isLastPage = true
            }
            is ApiResult.Error -> {
                val currentState = _uiState.value as? ProfileUiState.Success ?: return
                _uiState.value = currentState.copy(isPostsLoading = false, postsError = postResult.message)
            }
        }
        isFetchingPosts = false
    }

    fun loadMorePosts() {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        if (isFetchingPosts || isLastPage || currentState.isPostsLoading) return

        viewModelScope.launch {
            isFetchingPosts = true
            _uiState.value = currentState.copy(isLoadingMore = true)

            val lastPostId = currentState.posts.lastOrNull()?.id

            when (val result = getUserPostsUseCase(currentState.user.id, lastPostId)) {
                is ApiResult.Success -> {
                    val newPosts = result.data
                    if (newPosts.isEmpty()) isLastPage = true
                    _uiState.value = currentState.copy(
                        posts = currentState.posts + newPosts,
                        isLoadingMore = false
                    )
                }
                is ApiResult.Error -> _uiState.value = currentState.copy(isLoadingMore = false)
            }
            isFetchingPosts = false
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
