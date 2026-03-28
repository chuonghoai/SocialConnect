package com.example.frontend.presentation.screen.otherprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.FriendUseCase.AcceptFriendRequestUseCase
import com.example.frontend.domain.usecase.FriendUseCase.AddFriendUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase, 
    private val addFriendUseCase: AddFriendUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase
) : ViewModel() {

    private val targetUserId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _uiState = MutableStateFlow<OtherProfileUiState>(OtherProfileUiState.Loading)
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    private var currentPosts = emptyList<Post>()
    private var isLastPage = false
    private var isFetching = false

  fun load(isRefresh: Boolean = false) {
        if (targetUserId.isBlank()) {
            _uiState.value = OtherProfileUiState.Error("Không tìm thấy người dùng")
            return
        }

        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is OtherProfileUiState.Success) {
                _uiState.value = OtherProfileUiState.Loading
            }
            if (isRefresh) {
                isLastPage = false
                currentPosts = emptyList()
            }

            when (val userResult = getUserProfileUseCase(targetUserId)) {
                is ApiResult.Success -> {
                    val user = userResult.data
                    _uiState.value = OtherProfileUiState.Success(
                        user = user,
                        posts = currentPosts,
                        isPostsLoading = true
                    )

                    when (val postsResult = getUserPostsUseCase(targetUserId, null, isRefresh)) {
                        is ApiResult.Success -> {
                            currentPosts = postsResult.data
                            isLastPage = postsResult.data.isEmpty()
                            _uiState.value = OtherProfileUiState.Success(
                                user = user,
                                posts = currentPosts
                            )
                        }

                        is ApiResult.Error -> {
                            _uiState.value = OtherProfileUiState.Success(
                                user = user,
                                posts = currentPosts,
                                isPostsLoading = false,
                                error = postsResult.message
                            )
                        }
                    }
                }

                is ApiResult.Error -> {
                    _uiState.value = OtherProfileUiState.Error(
                        userResult.message.ifBlank { "Không thể tải trang cá nhân" }
                    )
                }
            }
        }
    }

    fun loadMorePosts() {
        val currentState = _uiState.value as? OtherProfileUiState.Success ?: return
        if (isFetching || isLastPage || currentState.isPostsLoading || currentState.isLoadingMore) return

        viewModelScope.launch {
            isFetching = true
            _uiState.value = currentState.copy(isLoadingMore = true)

            val lastPostId = currentPosts.lastOrNull()?.id
            when (val result = getUserPostsUseCase(targetUserId, lastPostId)) {
                is ApiResult.Success -> {
                    val newPosts = result.data
                    if (newPosts.isEmpty()) {
                        isLastPage = true
                    } else {
                        currentPosts = currentPosts + newPosts
                    }

                    val latestState = _uiState.value as? OtherProfileUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(
                        posts = currentPosts,
                        isLoadingMore = false,
                        error = null
                    )
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? OtherProfileUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(
                        isLoadingMore = false,
                        error = result.message.ifBlank { "Không thể tải thêm bài viết" }
                    )
                }
            }
            isFetching = false
        }
    }


    fun onFriendAction() {
        val currentState = _uiState.value as? OtherProfileUiState.Success ?: return
        val status = currentState.user.friendshipStatus

        viewModelScope.launch{
            when (status) {
                "NONE" -> addFriendUseCase(targetUserId)
                "REQUEST_RECEIVED" -> acceptFriendRequestUseCase(targetUserId)
                else -> return@launch
            }
            load(isRefresh = true)
        }
    }
}
