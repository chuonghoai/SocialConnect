package com.example.frontend.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetSavedPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetUserPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.LikePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SavePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SharePostUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
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
    private val likePostUseCase: LikePostUseCase,
    private val sharePostUseCase: SharePostUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val notificationManager: AppNotificationManager,
    private val postDetailStore: PostDetailStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var isFetchingPosts = false
    private var isLastPage = false

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

        if (index == 1 && currentState.savedPosts.isEmpty() && !currentState.isSavedPostsLoading) {
            loadSavedPosts(isRefresh = false)
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

        val ownerId = currentState.user.id
        val lastPostId = currentState.posts.lastOrNull()?.id

        viewModelScope.launch {
            isFetchingPosts = true
            val loadingState = _uiState.value as? ProfileUiState.Success
            if (loadingState != null) {
                _uiState.value = loadingState.copy(isLoadingMore = true)
            }

            when (val result = getUserPostsUseCase(ownerId, lastPostId)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    val newPosts = applySavedState(result.data, latestState.savedPosts)
                    if (newPosts.isEmpty()) isLastPage = true

                    _uiState.value = latestState.copy(
                        posts = (latestState.posts + newPosts).distinctBy { it.id },
                        isLoadingMore = false,
                        postsError = null
                    )
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(isLoadingMore = false)
                }
            }
            isFetchingPosts = false
        }
    }

    fun toggleSavePost(postId: String) {
        if (_uiState.value !is ProfileUiState.Success) return

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
                    notificationManager.showMessage(
                        message = result.message.ifBlank { "Không thể lưu bài viết" },
                        type = NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        val sourcePost = currentState.posts.find { it.id == postId }
            ?: currentState.savedPosts.find { it.id == postId }
            ?: return

        val newIsLiked = !sourcePost.isLiked
        val newLikeCount = if (newIsLiked) sourcePost.likeCount + 1 else sourcePost.likeCount - 1

        _uiState.value = updatePostState(currentState, postId) {
            it.copy(isLiked = newIsLiked, likeCount = newLikeCount)
        }

        viewModelScope.launch {
            when (val result = likePostUseCase(postId, newIsLiked, newLikeCount)) {
                is ApiResult.Success -> Unit

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    _uiState.value = updatePostState(latestState, postId) {
                        it.copy(isLiked = sourcePost.isLiked, likeCount = sourcePost.likeCount)
                    }
                    notificationManager.showMessage(
                        message = result.message.ifBlank { "Không thể thích bài viết" },
                        type = NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun sharePost(postId: String) {
        viewModelScope.launch {
            when (val result = sharePostUseCase(postId)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? ProfileUiState.Success ?: return@launch
                    _uiState.value = updatePostState(latestState, postId) {
                        it.copy(shareCount = it.shareCount + 1)
                    }
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

    fun selectPost(post: Post) {
        postDetailStore.selectedPost = post
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                logoutUseCase()
                onLoggedOut()
            } catch (_: Exception) {
                notificationManager.showMessage(
                    message = "Không thể đăng xuất. Vui lòng thử lại",
                    type = NotificationType.ERROR
                )
            }
        }
    }

    private fun updatePostState(
        state: ProfileUiState.Success,
        postId: String,
        transform: (Post) -> Post
    ): ProfileUiState.Success {
        val updatedPosts = state.posts.map { post ->
            if (post.id == postId) transform(post) else post
        }
        val updatedSavedPosts = state.savedPosts.map { post ->
            if (post.id == postId) transform(post) else post
        }
        return state.copy(posts = updatedPosts, savedPosts = updatedSavedPosts)
    }

    private fun applySavedState(posts: List<Post>, savedPosts: List<Post>): List<Post> {
        if (savedPosts.isEmpty()) return posts
        val savedIds = savedPosts.map { it.id }.toSet()
        return posts.map { post ->
            if (savedIds.contains(post.id)) post.copy(isSaved = true) else post
        }
    }
}
