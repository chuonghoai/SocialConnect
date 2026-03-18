package com.example.frontend.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.core.util.PostUploadManager
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.PostUseCase.GetNewsFeedUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetSavedPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.LikePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SavePostUseCase
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
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val savePostUseCase: SavePostUseCase,
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
    private var savedPostIds: Set<String> = emptySet()

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
                    savedPostIds = fetchSavedPostIds()
                    _uiState.value = HomeUiState.Success(posts = applySavedState(result.data))
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
                    val newPosts = applySavedState(result.data)
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
                    // no-op
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

    fun savePost(postId: String) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return

        viewModelScope.launch {
            when (val result = savePostUseCase(postId)) {
                is ApiResult.Success -> {
                    savedPostIds = if (result.data) {
                        savedPostIds + postId
                    } else {
                        savedPostIds - postId
                    }

                    val latestState = _uiState.value as? HomeUiState.Success ?: return@launch
                    val updatedPosts = latestState.posts.map { post ->
                        if (post.id == postId) post.copy(isSaved = result.data) else post
                    }
                    _uiState.value = latestState.copy(posts = updatedPosts)

                    val message = if (result.data) "Đã lưu bài viết" else "Đã bỏ lưu bài viết"
                    notificationManager.showMessage(message = message, type = NotificationType.SUCCESS)
                }

                is ApiResult.Error -> {
                    notificationManager.showMessage(
                        message = result.message.ifBlank { "Không thể lưu bài viết" },
                        type = NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun selectPost(post: Post) {
        postDetailStore.selectedPost = post
    }

    private suspend fun fetchSavedPostIds(): Set<String> {
        val ids = LinkedHashSet<String>()
        var afterId: String? = null

        while (true) {
            when (val result = getSavedPostsUseCase(afterId = afterId)) {
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) {
                        return ids
                    }

                    result.data.forEach { ids.add(it.id) }
                    afterId = result.data.lastOrNull()?.id
                }

                is ApiResult.Error -> {
                    return ids
                }
            }
        }
    }

    private fun applySavedState(posts: List<Post>): List<Post> {
        if (savedPostIds.isEmpty()) return posts
        return posts.map { post ->
            if (savedPostIds.contains(post.id)) post.copy(isSaved = true) else post
        }
    }
}
