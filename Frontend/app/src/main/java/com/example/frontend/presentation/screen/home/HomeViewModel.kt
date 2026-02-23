package com.example.frontend.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.GetNewsFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNewsFeedUseCase: GetNewsFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isFetching = false
    private var isLastPage = false

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            isLastPage = false
            isFetching = true

            when (val result = getNewsFeedUseCase(afterId = null)) {
                is ApiResult.Success -> {
                    _uiState.value = HomeUiState.Success(posts = result.data)
                    if (result.data.isEmpty()) isLastPage = true
                }
                is ApiResult.Error -> _uiState.value =
                    HomeUiState.Error(result.message.ifBlank { "Failed to load news feed" })
            }
            isFetching = false
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
}