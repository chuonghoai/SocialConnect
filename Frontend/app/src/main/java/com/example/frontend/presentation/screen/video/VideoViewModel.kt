package com.example.frontend.presentation.screen.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.PostRepository
import com.example.frontend.domain.usecase.PostUseCase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Loading)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var isFetching = false
    private var isLastPage = false

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            else _uiState.value = VideoUiState.Loading
            isLastPage = false
            isFetching = true

            when (val result = getVideosUseCase(afterId = null, isRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    _uiState.value = VideoUiState.Success(posts = result.data)
                    if (result.data.isEmpty()) isLastPage = true
                }
                is ApiResult.Error -> _uiState.value = VideoUiState.Error(result.message)
            }

            isFetching = false
            if (isRefresh) _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (isFetching || isLastPage) return
        val currentState = _uiState.value as? VideoUiState.Success ?: return

        viewModelScope.launch {
            isFetching = true
            _uiState.value = currentState.copy(isLoadingMore = true)
            val lastPostId = currentState.posts.lastOrNull()?.id

            when (val result = getVideosUseCase(afterId = lastPostId)) {
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
            isFetching = false
        }
    }

    fun onLikeVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return

        val newIsLiked = !targetVideo.isLiked
        val newLikeCount = if (newIsLiked) {
            targetVideo.likeCount + 1
        } else {
            (targetVideo.likeCount - 1).coerceAtLeast(0)
        }

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(isLiked = newIsLiked, likeCount = newLikeCount)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (postRepository.likeVideo(videoId, newIsLiked, newLikeCount)) {
                is ApiResult.Success -> {
                    // no-op
                }
                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(posts = currentState.posts)
                }
            }
        }
    }

    fun onSaveVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return

        val oldIsSaved = targetVideo.isSaved
        val optimisticIsSaved = !oldIsSaved

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(isSaved = optimisticIsSaved)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (val result = postRepository.saveVideo(videoId)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val syncedPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(isSaved = result.data)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = syncedPosts)
                }
                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val rolledBackPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(isSaved = oldIsSaved)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = rolledBackPosts)
                }
            }
        }
    }

    fun onShareVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return
        val oldShareCount = targetVideo.shareCount

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(shareCount = post.shareCount + 1)
            } else {
                post
            }
        }

        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (postRepository.shareVideo(videoId)) {
                is ApiResult.Success -> {
                    // no-op
                }
                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val rolledBackPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(shareCount = oldShareCount)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = rolledBackPosts)
                }
            }
        }
    }

    fun onCreateVideoComment(videoId: String, content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return

        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return
        val oldCommentCount = targetVideo.commentCount

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(commentCount = post.commentCount + 1)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (postRepository.createVideoComment(videoId, trimmedContent)) {
                is ApiResult.Success -> {
                    // no-op
                }
                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val rolledBackPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(commentCount = oldCommentCount)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = rolledBackPosts)
                }
            }
        }
    }
}
