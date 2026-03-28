package com.example.frontend.presentation.screen.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.AdminUserItem
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.AuthUseCase.DeleteUserUseCase
import com.example.frontend.domain.usecase.AuthUseCase.GetAdminUsersUseCase
import com.example.frontend.domain.usecase.AuthUseCase.LockUserUseCase
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.AuthUseCase.UnlockUserUseCase
import com.example.frontend.domain.usecase.PostUseCase.DeletePostByAdminUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetNewsFeedUseCase
import com.example.frontend.domain.usecase.PostUseCase.HidePostByAdminUseCase
import com.example.frontend.domain.usecase.PostUseCase.ShowPostByAdminUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val selectedTabIndex: Int = 0,
    val keyword: String = "",
    val users: List<AdminUserItem> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isUserLoading: Boolean = false,
    val isPostLoading: Boolean = false,
    val hiddenPostIds: Set<String> = emptySet(),
    val actionLoadingUserId: String? = null,
    val lockedUserIds: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val currentAdminName: String = "Admin",
    val currentAdminAvatarUrl: String? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val getAdminUsersUseCase: GetAdminUsersUseCase,
    private val lockUserUseCase: LockUserUseCase,
    private val unlockUserUseCase: UnlockUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getNewsFeedUseCase: GetNewsFeedUseCase,
    private val hidePostByAdminUseCase: HidePostByAdminUseCase,
    private val showPostByAdminUseCase: ShowPostByAdminUseCase,
    private val deletePostByAdminUseCase: DeletePostByAdminUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var allUsers: List<AdminUserItem> = emptyList()

    init {
        loadCurrentAdmin()
        loadUsers(isRefresh = false)
        loadPosts(isRefresh = false)
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index, message = null, error = null)
    }

    fun refreshCurrentTab() {
        if (_uiState.value.selectedTabIndex == 1) {
            loadPosts(isRefresh = true)
        } else {
            loadUsers(isRefresh = true)
        }
    }

    fun onKeywordChange(value: String) {
        _uiState.value = _uiState.value.copy(keyword = value, message = null, error = null)
        applyUserFilter(value)
    }

    fun searchUsers() {
        applyUserFilter(_uiState.value.keyword)
    }

    fun lockUser(userId: String) {
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(error = "Khong the khoa tai khoan hien tai")
            return
        }
        if (_uiState.value.lockedUserIds.contains(userId)) {
            _uiState.value = _uiState.value.copy(message = "User nay da bi khoa")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoadingUserId = userId, error = null, message = null)
            when (val result = lockUserUseCase(userId)) {
                is ApiResult.Success -> {
                    allUsers = allUsers.map { user ->
                        if (user.id == userId) user.copy(isBlock = true) else user
                    }
                    applyUserFilter(_uiState.value.keyword)
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        lockedUserIds = _uiState.value.lockedUserIds + userId,
                        message = "Da khoa tai khoan user"
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        error = result.message.ifBlank { "Khong the khoa tai khoan user" }
                    )
                }
            }
        }
    }

    fun unlockUser(userId: String) {
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(error = "Khong the mo khoa tai khoan hien tai")
            return
        }
        if (!_uiState.value.lockedUserIds.contains(userId)) {
            _uiState.value = _uiState.value.copy(message = "User nay chua bi khoa")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoadingUserId = userId, error = null, message = null)
            when (val result = unlockUserUseCase(userId)) {
                is ApiResult.Success -> {
                    allUsers = allUsers.map { user ->
                        if (user.id == userId) user.copy(isBlock = false) else user
                    }
                    applyUserFilter(_uiState.value.keyword)
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        lockedUserIds = _uiState.value.lockedUserIds - userId,
                        message = "Da mo khoa tai khoan user"
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        error = result.message.ifBlank { "Khong the mo khoa tai khoan user" }
                    )
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        if (userId == _uiState.value.currentUserId) {
            _uiState.value = _uiState.value.copy(error = "Khong the xoa tai khoan hien tai")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoadingUserId = userId, error = null, message = null)
            when (val result = deleteUserUseCase(userId)) {
                is ApiResult.Success -> {
                    allUsers = allUsers.filterNot { it.id == userId }
                    applyUserFilter(_uiState.value.keyword)
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        lockedUserIds = _uiState.value.lockedUserIds - userId,
                        message = "Da xoa tai khoan user"
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoadingUserId = null,
                        error = result.message.ifBlank { "Khong the xoa tai khoan user" }
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
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts.filterNot { it.id == postId },
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

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }

    private fun loadCurrentAdmin() {
        viewModelScope.launch {
            when (val meResult = getMeUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUserId = meResult.data.id,
                        currentAdminName = meResult.data.displayName.ifBlank { meResult.data.username },
                        currentAdminAvatarUrl = meResult.data.avatarUrl
                    )
                    applyUserFilter(_uiState.value.keyword)
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = meResult.message.ifBlank { "Khong the tai thong tin admin" }
                    )
                }
            }
        }
    }

    private fun loadUsers(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUserLoading = true, error = null, message = null)
            when (val result = getAdminUsersUseCase()) {
                is ApiResult.Success -> {
                    allUsers = result.data
                    applyUserFilter(_uiState.value.keyword)
                    _uiState.value = _uiState.value.copy(
                        isUserLoading = false,
                        lockedUserIds = result.data.filter { it.isBlock }.map { it.id }.toSet(),
                        message = if (isRefresh) "Da cap nhat danh sach user" else _uiState.value.message
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isUserLoading = false,
                        error = result.message.ifBlank { "Khong the tai danh sach user" }
                    )
                }
            }
        }
    }

    private fun loadPosts(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPostLoading = true, error = null, message = null)
            when (val result = getNewsFeedUseCase(afterId = null, isRefresh = isRefresh, includeHidden = true)) {
                is ApiResult.Success -> {
                    val hiddenPostIds = result.data
                        .filter { it.isHiddenByAdmin }
                        .map { it.id }
                        .toSet()
                    _uiState.value = _uiState.value.copy(
                        isPostLoading = false,
                        posts = result.data,
                        hiddenPostIds = hiddenPostIds
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPostLoading = false,
                        error = result.message.ifBlank { "Khong the tai danh sach bai viet" }
                    )
                }
            }
        }
    }

    private fun applyUserFilter(keyword: String) {
        val trimmed = keyword.trim()
        val currentUserId = _uiState.value.currentUserId
        val base = allUsers.filter { it.id != currentUserId }
        val filtered = if (trimmed.isBlank()) {
            base
        } else {
            base.filter { user ->
                user.displayName.contains(trimmed, ignoreCase = true) ||
                    user.username.contains(trimmed, ignoreCase = true) ||
                    user.email.contains(trimmed, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(
            users = filtered,
            message = if (trimmed.isNotBlank() && filtered.isEmpty()) "Khong tim thay user phu hop" else _uiState.value.message
        )
    }
}
