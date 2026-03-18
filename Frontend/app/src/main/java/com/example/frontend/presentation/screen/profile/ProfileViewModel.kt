package com.example.frontend.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetSavedPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.SavePostUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
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
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val savePostUseCase: SavePostUseCase,
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
                    val selectedTabIndex = currentState?.selectedTabIndex ?: 0

                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        selectedTabIndex = selectedTabIndex,
                        posts = if (isRefresh) emptyList() else currentState?.posts ?: emptyList(),
                        savedPosts = if (isRefresh) emptyList() else currentState?.savedPosts ?: emptyList(),
                        isPostsLoading = selectedTabIndex == 0,
                        isSavedPostsLoading = selectedTabIndex == 1
                    )

                    isLastPage = false
                    loadUserPosts(user.id, isRefresh)
                    if (selectedTabIndex == 1) {
                        loadSavedPosts(isRefresh)
                    }
                }

                is ApiResult.Error -> _uiState.value = ProfileUiState.Error(userResult.message)
            }

            if (isRefresh) _isRefreshing.value = false
        }
    }

    fun onTabSelected(index: Int) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        if (currentState.selectedTabIndex == index) return

        _uiState.value = currentState.copy(selectedTabIndex = index)

        if (index == 1 && currentState.savedPosts.isEmpty() && !currentState.isSavedPostsLoading) {
            loadSavedPosts(isRefresh = false)
        }
    }

    private suspend fun loadUserPosts(userId: String, isRefresh: Boolean) {
        isFetchingPosts = true
        when (val postResult = getUserPostsUseCase(userId, null, isRefresh)) {
            is ApiResult.Success -> {
                val currentState = _uiState.value as? ProfileUiState.Success ?: return
                _uiState.value = currentState.copy(
                    posts = applySavedState(postResult.data, currentState.savedPosts),
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

    private fun loadSavedPosts(isRefresh: Boolean) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isSavedPostsLoading = true,
                savedPostsError = null
            )

            when (val result = getSavedPostsUseCase(afterId = null, isRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    val savedPosts = result.data.map { it.copy(isSaved = true) }
                    _uiState.value = latestState.copy(
                        posts = applySavedState(latestState.posts, savedPosts),
                        savedPosts = savedPosts,
                        isSavedPostsLoading = false,
                        savedPostsError = null
                    )
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(
                        isSavedPostsLoading = false,
                        savedPostsError = result.message
                    )
                }
            }
        }
    }

    fun loadMorePosts() {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        if (currentState.selectedTabIndex != 0) return
        if (isFetchingPosts || isLastPage || currentState.isPostsLoading) return

        viewModelScope.launch {
            isFetchingPosts = true
            _uiState.value = currentState.copy(isLoadingMore = true)

            val lastPostId = currentState.posts.lastOrNull()?.id

            when (val result = getUserPostsUseCase(currentState.user.id, lastPostId)) {
                is ApiResult.Success -> {
                    val newPosts = applySavedState(result.data, currentState.savedPosts)
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

    fun toggleSavePost(postId: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return

        viewModelScope.launch {
            when (val result = savePostUseCase(postId)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    val updatedPosts = latestState.posts.map { post ->
                        if (post.id == postId) post.copy(isSaved = result.data) else post
                    }

                    val existingSaved = latestState.savedPosts.any { it.id == postId }
                    val updatedSavedPosts = if (result.data) {
                        if (existingSaved) {
                            latestState.savedPosts.map { post ->
                                if (post.id == postId) post.copy(isSaved = true) else post
                            }
                        } else {
                            val sourcePost = updatedPosts.find { it.id == postId }
                            if (sourcePost != null) listOf(sourcePost.copy(isSaved = true)) + latestState.savedPosts
                            else latestState.savedPosts
                        }
                    } else {
                        latestState.savedPosts.filterNot { it.id == postId }
                    }

                    _uiState.value = latestState.copy(
                        posts = updatedPosts,
                        savedPosts = updatedSavedPosts
                    )
                }

                is ApiResult.Error -> {
                    // keep current data, skip hard error UI
                }
            }
        }
    }

    private fun applySavedState(posts: List<Post>, savedPosts: List<Post>): List<Post> {
        if (savedPosts.isEmpty()) return posts
        val savedIds = savedPosts.map { it.id }.toSet()
        return posts.map { post ->
            if (savedIds.contains(post.id)) post.copy(isSaved = true) else post
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}

