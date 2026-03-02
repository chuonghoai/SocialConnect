package com.example.frontend.presentation.screen.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.PostUseCase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase
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
}