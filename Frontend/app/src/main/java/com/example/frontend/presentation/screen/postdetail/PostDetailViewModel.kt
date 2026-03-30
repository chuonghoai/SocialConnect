package com.example.frontend.presentation.screen.postdetail

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.data.mapper.toDomainPost
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.data.remote.dto.CreateCommentRequest
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.LikePostUseCase
import com.example.frontend.domain.usecase.PostUseCase.SharePostUseCase
import com.example.frontend.domain.usecase.FriendUseCase.GetShareFriendsUseCase
import com.example.frontend.presentation.screen.share.ShareFriendsUiState
import com.example.frontend.presentation.screen.share.SharePostSubmitData
import com.example.frontend.ui.component.NotificationType
import com.example.frontend.domain.model.normalizeVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postDetailStore: PostDetailStore,
    private val postApi: PostApi,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val sharePostUseCase: SharePostUseCase,
    private val getShareFriendsUseCase: GetShareFriendsUseCase,
    private val notificationManager: AppNotificationManager
) : ViewModel() {

    companion object {
        private const val COMMENT_PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _shareFriendsState = MutableStateFlow(ShareFriendsUiState())
    val shareFriendsState: StateFlow<ShareFriendsUiState> = _shareFriendsState.asStateFlow()

    private var currentCommentPage: Int = 0
    private var loadedShareFriendsForUserId: String? = null

    fun loadPostDetail(postId: String? = null) {
        viewModelScope.launch {
            val selected = postDetailStore.selectedPost
            val targetPost = when {
                selected != null && (postId.isNullOrBlank() || selected.id == postId) -> selected
                !postId.isNullOrBlank() -> {
                    try {
                        postApi.getPostById(postId).toDomainPost()
                    } catch (_: Exception) {
                        null
                    }
                }

                else -> null
            }

            if (targetPost == null) return@launch

            currentCommentPage = 0
            _uiState.update {
                it.copy(
                    post = targetPost,
                    isLiked = targetPost.isLiked,
                    likeCount = targetPost.likeCount,
                    commentCount = targetPost.commentCount,
                    isLoadingComments = true,
                    isLoadingMoreComments = false,
                    hasMoreComments = true,
                    commentsError = false,
                    comments = emptyList()
                )
            }
            loadCommentsPage(reset = true)
        }
    }

    fun retryLoadComments() {
        loadCommentsPage(reset = true)
    }

    fun loadMoreComments() {
        val state = _uiState.value
        if (state.isLoadingComments || state.isLoadingMoreComments || !state.hasMoreComments) return
        loadCommentsPage(reset = false)
    }

    private fun loadCommentsPage(reset: Boolean) {
        val post = _uiState.value.post ?: return
        if (reset) currentCommentPage = 0

        _uiState.update {
            if (reset) {
                it.copy(isLoadingComments = true, commentsError = false)
            } else {
                it.copy(isLoadingMoreComments = true, commentsError = false)
            }
        }

        viewModelScope.launch {
            try {
                val comments = postApi.getPostComments(
                    postId = post.id,
                    page = if (reset) 0 else currentCommentPage,
                    size = COMMENT_PAGE_SIZE
                ).map { it.toDomain() }

                val hasMore = comments.size >= COMMENT_PAGE_SIZE
                currentCommentPage = if (reset) 1 else currentCommentPage + 1

                _uiState.update {
                    val merged = if (reset) {
                        comments
                    } else {
                        (it.comments + comments).distinctBy { item -> item.id }
                    }

                    it.copy(
                        comments = merged,
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        hasMoreComments = hasMore
                    )
                }

                Log.d(
                    "PostDetail",
                    "Loaded ${comments.size} comments for postId=${post.id}, page=${if (reset) 0 else currentCommentPage - 1}"
                )
            } catch (e: Exception) {
                Log.e("PostDetail", "Failed to load comments for postId=${post.id}", e)
                _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = true
                    )
                }
            }
        }
    }

    fun toggleLike() {
        val state = _uiState.value
        val post = state.post ?: return
        val nowLiked = !state.isLiked
        val nowLikeCount = if (nowLiked) state.likeCount + 1 else (state.likeCount - 1).coerceAtLeast(0)

        _uiState.update { it.copy(isLiked = nowLiked, likeCount = nowLikeCount) }

        viewModelScope.launch {
            when (val result = likePostUseCase(post.id, nowLiked, nowLikeCount)) {
                is ApiResult.Success -> {
                    postDetailStore.selectedPost = post.copy(isLiked = nowLiked, likeCount = nowLikeCount)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLiked = !nowLiked, likeCount = state.likeCount) }
                    notificationManager.showMessage(result.message, NotificationType.ERROR)
                }
            }
        }
    }

    fun loadShareFriends(currentUserId: String, forceRefresh: Boolean = false) {
        if (currentUserId.isBlank()) return

        val current = _shareFriendsState.value
        if (loadedShareFriendsForUserId == currentUserId && !current.isLoading && current.error == null && !forceRefresh) return

        _shareFriendsState.value = current.copy(isLoading = true, error = null)

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

    fun sharePost(payload: SharePostSubmitData) {
        val postId = payload.postId
        viewModelScope.launch {
            val visibility = payload.privacy.normalizeVisibility()

            when (val result = sharePostUseCase(
                postId = postId,
                content = payload.shareText.trim().ifBlank { null },
                visibility = visibility
            )) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(post = it.post?.copy(shareCount = (it.post?.shareCount ?: 0) + 1)) }
                    
                    _uiState.value.post?.let { postDetailStore.selectedPost = it }

                    notificationManager.showMessage("Chia sẻ bài viết thành công", NotificationType.SUCCESS)
                }
                is ApiResult.Error -> {
                    notificationManager.showMessage(result.message, NotificationType.ERROR)
                }
            }
        }
    }

    fun onCommentInputChange(text: String) {
        _uiState.update { it.copy(commentInput = text) }
    }

    fun onMediaSelected(uris: List<Uri>) {
        _uiState.update {
            it.copy(selectedMediaUris = (it.selectedMediaUris + uris).distinct())
        }
    }

    fun removeSelectedMedia(uri: Uri) {
        _uiState.update { it.copy(selectedMediaUris = it.selectedMediaUris - uri) }
    }

    fun clearSelectedMedia() {
        _uiState.update { it.copy(selectedMediaUris = emptyList()) }
    }

    fun onReplyToComment(comment: Comment) {
        _uiState.update { it.copy(replyingToComment = comment) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingToComment = null) }
    }

    fun submitComment() {
        val state = _uiState.value
        val postId = state.post?.id ?: return
        val text = state.commentInput.trim()
        val selectedMediaUris = state.selectedMediaUris

        if (state.isSendingComment) return
        if (text.isBlank() && selectedMediaUris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }

            val mediaIds = mutableListOf<String>()

            if (selectedMediaUris.isNotEmpty()) {
                for (uri in selectedMediaUris) {
                    when (val uploadResult = uploadMediaUseCase(uri)) {
                        is ApiResult.Success -> mediaIds.add(uploadResult.data)
                        is ApiResult.Error -> {
                            _uiState.update { it.copy(isSendingComment = false) }
                            notificationManager.showMessage(
                                uploadResult.message,
                                NotificationType.ERROR
                            )
                            return@launch
                        }
                    }
                }
            }

            try {
                val response = postApi.createComment(
                    postId = postId,
                    request = CreateCommentRequest(
                        content = text,
                        parentCommentId = state.replyingToComment?.id,
                        mediaId = mediaIds.firstOrNull(),
                        mediaIds = mediaIds.ifEmpty { null }
                    )
                )

                if (!response.isSuccessful) {
                    throw IllegalStateException("Create comment failed (${response.code()})")
                }

                _uiState.update {
                    val updatedPost = it.post?.copy(commentCount = it.commentCount + 1)
                    if (updatedPost != null) postDetailStore.selectedPost = updatedPost
                    
                    it.copy(
                        post = updatedPost,
                        isSendingComment = false,
                        commentInput = "",
                        selectedMediaUris = emptyList(),
                        replyingToComment = null,
                        commentCount = it.commentCount + 1
                    )
                }

                loadCommentsPage(reset = true)
            } catch (e: Exception) {
                Log.e("PostDetail", "Failed to submit comment for postId=$postId", e)
                _uiState.update { it.copy(isSendingComment = false) }
                notificationManager.showMessage(
                    "Không thể gửi bình luận. Vui lòng thử lại.",
                    NotificationType.ERROR
                )
            }
        }
    }
}
