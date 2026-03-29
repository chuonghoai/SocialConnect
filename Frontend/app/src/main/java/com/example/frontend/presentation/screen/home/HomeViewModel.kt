package com.example.frontend.presentation.screen.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.core.util.PostUploadManager
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.domain.usecase.FriendUseCase.GetShareFriendsUseCase
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetNewsFeedUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetSavedPostsUseCase
import com.example.frontend.domain.usecase.PostUseCase.LikePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.ReportPostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SavePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SharePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.UpdatePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.DeletePostUseCase
import com.example.frontend.presentation.screen.share.ShareFriendsUiState
import com.example.frontend.presentation.screen.share.SharePostSubmitData
import com.example.frontend.ui.component.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNewsFeedUseCase: GetNewsFeedUseCase,
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val savePostUseCase: SavePostUseCase,
    private val sharePostUseCase: SharePostUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getShareFriendsUseCase: GetShareFriendsUseCase,
    private val notificationManager: AppNotificationManager,
    private val postUploadManager: PostUploadManager,
    private val postDetailStore: PostDetailStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    private val _shareFriendsState = MutableStateFlow(ShareFriendsUiState())
    val uploadState = postUploadManager.uploadState
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val shareFriendsState: StateFlow<ShareFriendsUiState> = _shareFriendsState.asStateFlow()

    private var isFetching = false
    private var isLastPage = false
    private var savedPostIds: Set<String> = emptySet()
    private var lastHandledPostCreatedTick: Long = 0L
    private var loadedShareFriendsForUserId: String? = null

    init {
        viewModelScope.launch {
            postUploadManager.postCreatedTick.collect { tick ->
                if (tick <= 0L || tick == lastHandledPostCreatedTick) return@collect
                lastHandledPostCreatedTick = tick
                val hasLoadedPosts = _uiState.value is HomeUiState.Success
                load(isRefresh = hasLoadedPosts)
            }
        }
    }

    init {
        viewModelScope.launch {
            postUploadManager.postCreatedEvents.collectLatest {
                load(isRefresh = true)
            }
        }
    }

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
                val newLikeCount = if (newIsLiked) {
                    post.likeCount + 1
                } else {
                    (post.likeCount - 1).coerceAtLeast(0)
                }

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

    fun sharePost(payload: SharePostSubmitData) {
        val postId = payload.postId
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
                    val latestState = _uiState.value as? HomeUiState.Success ?: return@launch
                    val updatedPosts = latestState.posts.map { post ->
                        if (post.id == postId) post.copy(shareCount = post.shareCount + 1) else post
                    }
                    _uiState.value = latestState.copy(posts = updatedPosts)

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

    fun editPost(
        postId: String,
        newContent: String,
        newVisibility: String,
        keptExistingMedia: List<PostMedia>,
        newMediaUris: List<Uri>
    ) {
        val content = newContent.trim()
        val keptMediaPublicIds = keptExistingMedia
            .mapNotNull { it.publicId?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val keptMediaUrls = keptExistingMedia
            .map { it.resolvedUrl().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (content.isBlank() && keptMediaUrls.isEmpty() && newMediaUris.isEmpty()) {
            notificationManager.showMessage(
                "Bài viết phải có nội dung hoặc ít nhất một ảnh/video",
                NotificationType.WARNING
            )
            return
        }

        viewModelScope.launch {
            val uploadedMediaIds = mutableListOf<String>()
            for (uri in newMediaUris) {
                when (val uploadResult = uploadMediaUseCase(uri)) {
                    is ApiResult.Success -> uploadedMediaIds.add(uploadResult.data)

                    is ApiResult.Error -> {
                        notificationManager.showMessage(
                            uploadResult.message.ifBlank { "Không thể upload ảnh/video mới" },
                            NotificationType.ERROR
                        )
                        return@launch
                    }
                }
            }

            val finalMediaPublicIds = (keptMediaPublicIds + uploadedMediaIds).distinct()

            when (
                val result = updatePostUseCase(
                    postId = postId,
                    content = content,
                    visibility = newVisibility,
                    mediaPublicIds = finalMediaPublicIds,
                    mediaUrls = keptMediaUrls
                )
            ) {
                is ApiResult.Success -> {
                    notificationManager.showMessage("Đã cập nhật bài viết", NotificationType.SUCCESS)
                    load(isRefresh = true)
                }

                is ApiResult.Error -> {
                    notificationManager.showMessage(
                        result.message.ifBlank { "Không thể sửa bài viết" },
                        NotificationType.ERROR
                    )
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
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val oldPosts = currentState.posts
        val updatedPosts = oldPosts.filterNot { it.id == postId }
        _uiState.value = currentState.copy(posts = updatedPosts)

        viewModelScope.launch {
            when (val result = deletePostUseCase(postId)) {
                is ApiResult.Success -> {
                    notificationManager.showMessage("Đã xóa bài viết", NotificationType.SUCCESS)
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? HomeUiState.Success
                    if (latestState != null) {
                        _uiState.value = latestState.copy(posts = oldPosts)
                    }
                    notificationManager.showMessage(
                        result.message.ifBlank { "Không thể xóa bài viết" },
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun hidePost(postId: String) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val updatedPosts = currentState.posts.filterNot { it.id == postId }
        _uiState.value = currentState.copy(posts = updatedPosts)
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
