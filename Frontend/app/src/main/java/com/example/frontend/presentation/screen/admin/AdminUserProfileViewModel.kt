package com.example.frontend.presentation.screen.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.AuthUseCase.DeleteUserUseCase
import com.example.frontend.domain.usecase.AuthUseCase.LockUserUseCase
import com.example.frontend.domain.usecase.AuthUseCase.UnlockUserUseCase
import com.example.frontend.domain.usecase.PostUseCase.DeletePostByAdminUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.HidePostByAdminUseCase
import com.example.frontend.domain.usecase.PostUseCase.ShowPostByAdminUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUserProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val hiddenPostIds: Set<String> = emptySet(),
    val isPostsLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLocked: Boolean = false,
    val isActionLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val postError: String? = null
)

@HiltViewModel
class AdminUserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val lockUserUseCase: LockUserUseCase,
    private val unlockUserUseCase: UnlockUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val hidePostByAdminUseCase: HidePostByAdminUseCase,
    private val showPostByAdminUseCase: ShowPostByAdminUseCase,
    private val deletePostByAdminUseCase: DeletePostByAdminUseCase
) : ViewModel() {

    private val targetUserId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _uiState = MutableStateFlow(
        AdminUserProfileUiState(isLocked = readInitialLocked(savedStateHandle))
    )
    val uiState: StateFlow<AdminUserProfileUiState> = _uiState.asStateFlow()

    private var currentPosts = emptyList<Post>()
    private var isLastPage = false
    private var isFetching = false

    init {
        load(isRefresh = true)
    }

    fun load(isRefresh: Boolean = false) {
        if (targetUserId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Khong tim thay nguoi dung"
            )
            return
        }

        viewModelScope.launch {
            if (isRefresh) {
                isLastPage = false
                currentPosts = emptyList()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                message = null,
                postError = null
            )

            when (val userResult = getUserProfileUseCase(targetUserId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = userResult.data,
                        isPostsLoading = true
                    )
                    loadPosts(isRefresh = isRefresh)
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = userResult.message.ifBlank { "Khong the tai trang ca nhan" }
                    )
                }
            }
        }
    }

    fun loadMorePosts() {
        val state = _uiState.value
        if (state.user == null || isFetching || isLastPage || state.isPostsLoading || state.isLoadingMore) return

        viewModelScope.launch {
            isFetching = true
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val lastPostId = currentPosts.lastOrNull()?.id
            when (val result = getUserPostsUseCase(targetUserId, lastPostId, includeHidden = true)) {
                is ApiResult.Success -> {
                    val newPosts = result.data
                    if (newPosts.isEmpty()) {
                        isLastPage = true
                    } else {
                        currentPosts = currentPosts + newPosts
                    }
                    _uiState.value = _uiState.value.copy(
                        posts = currentPosts,
                        hiddenPostIds = currentPosts.filter { it.isHiddenByAdmin }.map { it.id }.toSet(),
                        isLoadingMore = false,
                        postError = null
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        postError = result.message.ifBlank { "Khong the tai them bai viet" }
                    )
                }
            }
            isFetching = false
        }
    }

    fun toggleUserLock() {
        val state = _uiState.value
        if (state.isActionLoading || targetUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isActionLoading = true, error = null, message = null)
            val result = if (state.isLocked) {
                unlockUserUseCase(targetUserId)
            } else {
                lockUserUseCase(targetUserId)
            }

            when (result) {
                is ApiResult.Success -> {
                    val nowLocked = !state.isLocked
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        isLocked = nowLocked,
                        message = if (nowLocked) "Da khoa tai khoan user" else "Da mo khoa tai khoan user"
                    )
                    loadPosts(isRefresh = true)
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = result.message.ifBlank {
                            if (state.isLocked) "Khong the mo khoa user" else "Khong the khoa user"
                        }
                    )
                }
            }
        }
    }

    fun deleteUser(onDeleted: () -> Unit) {
        val state = _uiState.value
        if (state.isActionLoading || targetUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isActionLoading = true, error = null, message = null)
            when (val result = deleteUserUseCase(targetUserId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        message = "Da xoa tai khoan user"
                    )
                    onDeleted()
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = result.message.ifBlank { "Khong the xoa user" }
                    )
                }
            }
        }
    }

    fun togglePostVisibility(postId: String) {
        val currentlyHidden = _uiState.value.hiddenPostIds.contains(postId)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, message = null)
            val result = if (currentlyHidden) {
                showPostByAdminUseCase(postId)
            } else {
                hidePostByAdminUseCase(postId)
            }

            when (result) {
                is ApiResult.Success -> {
                    val hidden = result.data
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts.map { post ->
                            if (post.id == postId) post.copy(isHiddenByAdmin = hidden) else post
                        },
                        hiddenPostIds = if (hidden) {
                            _uiState.value.hiddenPostIds + postId
                        } else {
                            _uiState.value.hiddenPostIds - postId
                        },
                        message = if (hidden) "Da an bai viet" else "Da hien bai viet"
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message.ifBlank {
                            if (currentlyHidden) "Khong the hien bai viet" else "Khong the an bai viet"
                        }
                    )
                }
            }
        }
    }

    fun removePost(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, message = null)
            when (val result = deletePostByAdminUseCase(postId)) {
                is ApiResult.Success -> {
                    currentPosts = currentPosts.filterNot { it.id == postId }
                    _uiState.value = _uiState.value.copy(
                        posts = currentPosts,
                        hiddenPostIds = _uiState.value.hiddenPostIds - postId,
                        message = "Da xoa bai viet"
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message.ifBlank { "Khong the xoa bai viet" }
                    )
                }
            }
        }
    }

    private fun loadPosts(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                isLastPage = false
                currentPosts = emptyList()
            }
            _uiState.value = _uiState.value.copy(isPostsLoading = true, postError = null)

            when (val postsResult = getUserPostsUseCase(
                userId = targetUserId,
                afterId = null,
                isRefresh = isRefresh,
                includeHidden = true
            )) {
                is ApiResult.Success -> {
                    currentPosts = postsResult.data
                    isLastPage = postsResult.data.isEmpty()
                    _uiState.value = _uiState.value.copy(
                        posts = currentPosts,
                        hiddenPostIds = currentPosts.filter { it.isHiddenByAdmin }.map { it.id }.toSet(),
                        isPostsLoading = false
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPostsLoading = false,
                        postError = postsResult.message.ifBlank { "Khong the tai bai viet" }
                    )
                }
            }
        }
    }

    private fun readInitialLocked(savedStateHandle: SavedStateHandle): Boolean {
        val rawArg = savedStateHandle.get<Any?>("locked") ?: return false
        return when (rawArg) {
            is Boolean -> rawArg
            is String -> rawArg.equals("true", ignoreCase = true) || rawArg == "1"
            is Number -> rawArg.toInt() != 0
            else -> false
        }
    }
}
