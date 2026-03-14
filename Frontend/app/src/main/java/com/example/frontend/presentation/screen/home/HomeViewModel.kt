package com.example.frontend.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.core.util.PostUploadManager
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.PostUseCase.GetNewsFeedUseCase
import com.example.frontend.domain.usecase.PostUseCase.LikePostUseCase
import com.example.frontend.ui.component.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNewsFeedUseCase: GetNewsFeedUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val notificationManager: AppNotificationManager,
    private val postUploadManager: PostUploadManager,
    private val postDetailStore: PostDetailStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    val uploadState = postUploadManager.uploadState
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isFetching = false
    private var isLastPage = false

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = HomeUiState.Loading
            }

            isLastPage = false
            isFetching = true

            when (val result = getNewsFeedUseCase(afterId = null, isRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    _uiState.value = HomeUiState.Success(posts = result.data)
                    if (result.data.isEmpty()) isLastPage = true
                }
                is ApiResult.Error -> _uiState.value = HomeUiState.Error(result.message)
            }

            isFetching = false
            if (isRefresh) _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (isFetching || isLastPage) return

        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success) return

        viewModelScope.launch {
            isFetching = true
            _uiState.value = currentState.copy(isLoadingMore = true)

            val lastPostId = currentState.posts.lastOrNull()?.id

            when (val result = getNewsFeedUseCase(afterId = lastPostId)) {
                is ApiResult.Success -> {
                    val newPosts = result.data
                    if (newPosts.isEmpty()) {
                        isLastPage = true
                        _uiState.value = currentState.copy(isLoadingMore = false)
                    } else {
                        val updatedPosts = currentState.posts + newPosts
                        _uiState.value = HomeUiState.Success(posts = updatedPosts, isLoadingMore = false)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            }
            isFetching = false
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success) return

        val originalPosts = currentState.posts

        var targetIsLiked = false
        var targetLikeCount = 0

        val updatedPosts = currentState.posts.map { post ->
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                val newLikeCount = if (newIsLiked) post.likeCount + 1 else post.likeCount - 1

                targetIsLiked = newIsLiked
                targetLikeCount = newLikeCount

                post.copy(isLiked = newIsLiked, likeCount = newLikeCount)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = updatedPosts)

        viewModelScope.launch {
            when (val result = likePostUseCase(postId, targetIsLiked, targetLikeCount)) {
                is ApiResult.Success -> {
                    //
                }
                is ApiResult.Error -> {
                    val currentLatestState = _uiState.value
                    if (currentLatestState is HomeUiState.Success) {
                        _uiState.value = currentLatestState.copy(posts = originalPosts)
                    }

                    notificationManager.showMessage(
                        message = result.message.ifBlank { "Lỗi kết nối mạng" },
                        type = NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun selectPost(post: Post) {
        postDetailStore.selectedPost = post
    }
}