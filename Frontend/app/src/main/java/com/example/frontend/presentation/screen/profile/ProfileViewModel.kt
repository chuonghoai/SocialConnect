package com.example.frontend.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.FriendUseCase.GetShareFriendsUseCase
import com.example.frontend.domain.usecase.PostUseCase.DeletePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetSavedPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.ReportPostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SavePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SharePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.UpdatePostUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import com.example.frontend.presentation.screen.share.ShareFriendsUiState
import com.example.frontend.presentation.screen.share.SharePostSubmitData
import com.example.frontend.ui.component.NotificationType
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
    private val sharePostUseCase: SharePostUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getShareFriendsUseCase: GetShareFriendsUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val notificationManager: AppNotificationManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    private val _shareFriendsState = MutableStateFlow(ShareFriendsUiState())
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    val shareFriendsState: StateFlow<ShareFriendsUiState> = _shareFriendsState.asStateFlow()
    private var isFetchingPosts = false
    private var isLastPage = false
    private var loadedShareFriendsForUserId: String? = null

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

        if (index == 1 && !currentState.isSavedPostsLoading) {
            loadSavedPosts(isRefresh = true)
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

    fun sharePost(payload: SharePostSubmitData) {
        val postId = payload.postId
        if (_uiState.value !is ProfileUiState.Success) return

        viewModelScope.launch {
            if (payload.selectedFriendIds.isNotEmpty()) {
                notificationManager.showMessage(
                    message = "BE chưa hỗ trợ gửi riêng cho bạn bè đã chọn, hiện chỉ chia sẻ bài viết lên bảng feed.",
                    type = NotificationType.WARNING
                )
            }

            val visibility = when (payload.privacy.lowercase()) {
                "only_me" -> "PRIVATE"
                "friends", "ban_be", "banbe" -> "FRIENDS"
                "public", "cong_khai", "congkhai" -> "PUBLIC"
                else -> payload.privacy
            }

            when (
                val result = sharePostUseCase(
                    postId = postId,
                    content = payload.shareText.trim().ifBlank { null },
                    visibility = visibility
                )
            ) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    val updatedPosts = latestState.posts.map { post ->
                        if (post.id == postId) post.copy(shareCount = post.shareCount + 1) else post
                    }
                    val updatedSavedPosts = latestState.savedPosts.map { post ->
                        if (post.id == postId) post.copy(shareCount = post.shareCount + 1) else post
                    }

                    _uiState.value = latestState.copy(
                        posts = updatedPosts,
                        savedPosts = updatedSavedPosts
                    )

                    notificationManager.showMessage(
                        message = "Chia sẻ bài viết thành công",
                        type = NotificationType.SUCCESS
                    )
                }

                is ApiResult.Error -> {
                    notificationManager.showMessage(
                        message = result.message.ifBlank { "Không thể chia sẻ bài viết" },
                        type = NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun hidePost(postId: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = currentState.copy(
            posts = currentState.posts.filterNot { it.id == postId },
            savedPosts = currentState.savedPosts.filterNot { it.id == postId }
        )
        notificationManager.showMessage("Đã ẩn bài viết", NotificationType.SUCCESS)
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            when (val result = reportPostUseCase(postId, reason)) {
                is ApiResult.Success -> {
                    notificationManager.showMessage("Đã gửi báo cáo bài viết", NotificationType.SUCCESS)
                }

                is ApiResult.Error -> {
                    val message = if (result.code == 404 || result.code == 501) {
                        "BE chưa hỗ trợ API báo cáo bài viết"
                    } else {
                        result.message.ifBlank { "Không thể báo cáo bài viết" }
                    }
                    notificationManager.showMessage(message, NotificationType.ERROR)
                }
            }
        }
    }

    fun loadShareFriends(currentUserId: String, forceRefresh: Boolean = false) {
        if (currentUserId.isBlank()) {
            _shareFriendsState.value = ShareFriendsUiState(
                friends = emptyList(),
                isLoading = false,
                error = "Không xác định người dùng hiện tại"
            )
            return
        }

        val current = _shareFriendsState.value
        val alreadyLoaded =
            loadedShareFriendsForUserId == currentUserId && !current.isLoading && current.error == null
        if (alreadyLoaded && !forceRefresh) return
        if (current.isLoading && !forceRefresh) return

        _shareFriendsState.value = current.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            when (val result = getShareFriendsUseCase(currentUserId)) {
                is ApiResult.Success -> {
                    loadedShareFriendsForUserId = currentUserId
                    _shareFriendsState.value = ShareFriendsUiState(
                        friends = result.data,
                        isLoading = false,
                        error = null
                    )
                }

                is ApiResult.Error -> {
                    _shareFriendsState.value = current.copy(
                        isLoading = false,
                        error = result.message.ifBlank { "Không thể tải danh sách bạn bè" }
                    )
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

    fun changePostVisibility(postId: String, visibility: String) {
        viewModelScope.launch {
            when (val result = updatePostUseCase(postId = postId, visibility = visibility)) {
                is ApiResult.Success -> {
                    notificationManager.showMessage("Đã đổi quyền bài viết", NotificationType.SUCCESS)
                }
                is ApiResult.Error -> {
                    val message = result.message.ifBlank { "Không thể đổi quyền bài viết" }
                    notificationManager.showMessage(message, NotificationType.ERROR)
                }
            }
        }
    }

    fun deletePost(postId: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        val oldPosts = currentState.posts
        val oldSaved = currentState.savedPosts

        val updatedPosts = oldPosts.filterNot { it.id == postId }
        val updatedSavedPosts = oldSaved.filterNot { it.id == postId }
        _uiState.value = currentState.copy(posts = updatedPosts, savedPosts = updatedSavedPosts)

        viewModelScope.launch {
            when (val result = deletePostUseCase(postId)) {
                is ApiResult.Success -> {
                    notificationManager.showMessage("Đã xóa bài viết", NotificationType.SUCCESS)
                }
                is ApiResult.Error -> {
                    val latestState = _uiState.value as? ProfileUiState.Success
                    if (latestState != null) {
                        _uiState.value = latestState.copy(posts = oldPosts, savedPosts = oldSaved)
                    }
                    notificationManager.showMessage(
                        result.message.ifBlank { "Không thể xóa bài viết" },
                        NotificationType.ERROR
                    )
                }
            }
        }
    }
}

