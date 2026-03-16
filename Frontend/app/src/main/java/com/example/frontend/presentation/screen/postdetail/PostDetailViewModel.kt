package com.example.frontend.presentation.screen.postdetail

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.data.remote.dto.CreateCommentRequest
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.ui.component.NotificationType
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
    private val notificationManager: AppNotificationManager
) : ViewModel() {

    companion object {
        private const val COMMENT_PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var currentCommentPage: Int = 0

    // ── Load post + comments ───────────────────────────────────────────────────
    fun loadPostDetail() {
        val post = postDetailStore.selectedPost ?: return
        currentCommentPage = 0
        _uiState.update {
            it.copy(
                post = post,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isLoadingComments = true,
                isLoadingMoreComments = false,
                hasMoreComments = true,
                commentsError = false,
                comments = emptyList()
            )
        }
        loadCommentsPage(reset = true)
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

    // ── Actions ───────────────────────────────────────────────────────────────
    fun toggleLike() {
        _uiState.update { state ->
            val nowLiked = !state.isLiked
            state.copy(
                isLiked = nowLiked,
                likeCount = if (nowLiked) state.likeCount + 1 else state.likeCount - 1
            )
        }
    }

    fun onCommentInputChange(text: String) {
        _uiState.update { it.copy(commentInput = text) }
    }

    fun onMediaSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedMediaUri = uri) }
    }

    fun removeSelectedMedia() {
        _uiState.update { it.copy(selectedMediaUri = null) }
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
        val selectedMediaUri = state.selectedMediaUri

        if (state.isSendingComment) return
        if (text.isBlank() && selectedMediaUri == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }

            var mediaId: String? = null

            if (selectedMediaUri != null) {
                when (val uploadResult = uploadMediaUseCase(selectedMediaUri)) {
                    is ApiResult.Success -> {
                        mediaId = uploadResult.data
                    }

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

            try {
                val response = postApi.createComment(
                    postId = postId,
                    request = CreateCommentRequest(
                        content = text,
                        parentCommentId = state.replyingToComment?.id,
                        mediaId = mediaId
                    )
                )

                if (!response.isSuccessful) {
                    throw IllegalStateException("Create comment failed (${response.code()})")
                }

                _uiState.update {
                    it.copy(
                        isSendingComment = false,
                        commentInput = "",
                        selectedMediaUri = null,
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
