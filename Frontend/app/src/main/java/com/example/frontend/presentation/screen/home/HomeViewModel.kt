package com.example.frontend.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.GetNewsFeedUseCase
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNewsFeedUseCase: GetNewsFeedUseCase,
    private val postDetailStore: PostDetailStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = getNewsFeedUseCase()) {
                is ApiResult.Success -> _uiState.value = HomeUiState.Success(result.data)
                is ApiResult.Error -> _uiState.value =
                    HomeUiState.Error(result.message.ifBlank { "Failed to load news feed" })
            }
        }
    }

    /** Lưu post được chọn vào store trước khi navigate sang PostDetailScreen */
    fun selectPost(post: Post) {
        postDetailStore.selectedPost = post
    }
}