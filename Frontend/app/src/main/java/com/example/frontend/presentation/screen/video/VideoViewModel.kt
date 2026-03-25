package com.example.frontend.presentation.screen.video

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.repository.PostRepository
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val postRepository: PostRepository,
    private val uploadMediaUseCase: UploadMediaUseCase
) : ViewModel() {
    companion object {
        private const val VIDEO_COMMENT_PAGE_SIZE = 50
    }

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Loading)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _commentsSheetState = MutableStateFlow(VideoCommentsSheetState())
    val commentsSheetState: StateFlow<VideoCommentsSheetState> = _commentsSheetState.asStateFlow()

    private var isFetching = false
    private var isLastPage = false

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            else _uiState.value = VideoUiState.Loading
            isLastPage = false
            isFetching = true

            when (val result = getVideosUseCase(afterId = null, isRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    _uiState.value = VideoUiState.Success(posts = result.data)
                    if (result.data.isEmpty()) isLastPage = true
                }

                is ApiResult.Error -> _uiState.value = VideoUiState.Error(result.message)
            }

            isFetching = false
            if (isRefresh) _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (isFetching || isLastPage) return
        val currentState = _uiState.value as? VideoUiState.Success ?: return

        viewModelScope.launch {
            isFetching = true
            _uiState.value = currentState.copy(isLoadingMore = true)
            val lastPostId = currentState.posts.lastOrNull()?.id

            when (val result = getVideosUseCase(afterId = lastPostId)) {
                is ApiResult.Success -> {
                    val newPosts = result.data
                    if (newPosts.isEmpty()) isLastPage = true
                    _uiState.value = currentState.copy(
                        posts = currentState.posts + newPosts,
                        isLoadingMore = false
                    )
                }

                is ApiResult.Error -> _uiState.value = currentState.copy(isLoadingMore = false)
            }
            isFetching = false
        }
    }

    fun onLikeVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return

        val newIsLiked = !targetVideo.isLiked
        val newLikeCount = if (newIsLiked) {
            targetVideo.likeCount + 1
        } else {
            (targetVideo.likeCount - 1).coerceAtLeast(0)
        }

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(isLiked = newIsLiked, likeCount = newLikeCount)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (postRepository.likeVideo(videoId, newIsLiked, newLikeCount)) {
                is ApiResult.Success -> {
                    // no-op
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(posts = currentState.posts)
                }
            }
        }
    }

    fun onSaveVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return

        val oldIsSaved = targetVideo.isSaved
        val optimisticIsSaved = !oldIsSaved

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(isSaved = optimisticIsSaved)
            } else {
                post
            }
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (val result = postRepository.saveVideo(videoId)) {
                is ApiResult.Success -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val syncedPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(isSaved = result.data)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = syncedPosts)
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val rolledBackPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(isSaved = oldIsSaved)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = rolledBackPosts)
                }
            }
        }
    }

    fun onShareVideo(videoId: String) {
        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return
        val oldShareCount = targetVideo.shareCount

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) {
                post.copy(shareCount = post.shareCount + 1)
            } else {
                post
            }
        }

        _uiState.value = currentState.copy(posts = optimisticPosts)

        viewModelScope.launch {
            when (postRepository.shareVideo(videoId)) {
                is ApiResult.Success -> {
                    // no-op
                }

                is ApiResult.Error -> {
                    val latestState = _uiState.value as? VideoUiState.Success ?: return@launch
                    val rolledBackPosts = latestState.posts.map { post ->
                        if (post.id == videoId) {
                            post.copy(shareCount = oldShareCount)
                        } else {
                            post
                        }
                    }
                    _uiState.value = latestState.copy(posts = rolledBackPosts)
                }
            }
        }
    }

    fun openComments(videoId: String) {
        _commentsSheetState.value = VideoCommentsSheetState(
            isVisible = true,
            videoId = videoId,
            isLoading = true
        )
        loadVideoComments(videoId)
    }

    fun closeComments() {
        _commentsSheetState.value = VideoCommentsSheetState()
    }

    fun onCommentInputChange(text: String) {
        _commentsSheetState.value = _commentsSheetState.value.copy(commentInput = text)
    }

    fun onReplyToComment(comment: Comment) {
        _commentsSheetState.value = _commentsSheetState.value.copy(replyingToComment = comment)
    }

    fun cancelReply() {
        _commentsSheetState.value = _commentsSheetState.value.copy(replyingToComment = null)
    }

    fun onMediaSelected(uri: Uri?) {
        _commentsSheetState.value = _commentsSheetState.value.copy(selectedMediaUri = uri)
    }

    fun clearSelectedMedia() {
        _commentsSheetState.value = _commentsSheetState.value.copy(selectedMediaUri = null)
    }

    fun submitVideoComment() {
        val sheetState = _commentsSheetState.value
        val videoId = sheetState.videoId ?: return
        val trimmedContent = sheetState.commentInput.trim()
        val selectedMediaUri = sheetState.selectedMediaUri
        if ((trimmedContent.isEmpty() && selectedMediaUri == null) || sheetState.isSubmitting) return

        val currentState = _uiState.value as? VideoUiState.Success ?: return
        val targetVideo = currentState.posts.find { it.id == videoId } ?: return
        val oldCommentCount = targetVideo.commentCount

        val optimisticPosts = currentState.posts.map { post ->
            if (post.id == videoId) post.copy(commentCount = post.commentCount + 1) else post
        }
        _uiState.value = currentState.copy(posts = optimisticPosts)
        _commentsSheetState.value = sheetState.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            var mediaId: String? = null
            if (selectedMediaUri != null) {
                when (val mediaUploadResult = uploadMediaUseCase(selectedMediaUri)) {
                    is ApiResult.Success -> mediaId = mediaUploadResult.data
                    is ApiResult.Error -> {
                        rollbackCommentCount(videoId, oldCommentCount)
                        _commentsSheetState.value = _commentsSheetState.value.copy(
                            isSubmitting = false,
                            errorMessage = mediaUploadResult.message
                        )
                        return@launch
                    }
                }
            }

            when (
                postRepository.createVideoComment(
                    videoId = videoId,
                    content = trimmedContent,
                    parentCommentId = sheetState.replyingToComment?.id,
                    mediaId = mediaId
                )
            ) {
                is ApiResult.Success -> {
                    _commentsSheetState.value = _commentsSheetState.value.copy(
                        isSubmitting = false,
                        commentInput = "",
                        replyingToComment = null,
                        selectedMediaUri = null,
                        errorMessage = null
                    )
                    loadVideoComments(videoId)
                }

                is ApiResult.Error -> {
                    rollbackCommentCount(videoId, oldCommentCount)
                    _commentsSheetState.value = _commentsSheetState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Khong the gui binh luan"
                    )
                }
            }
        }
    }

    private fun rollbackCommentCount(videoId: String, oldCommentCount: Int) {
        val latestState = _uiState.value as? VideoUiState.Success ?: return
        val rolledBackPosts = latestState.posts.map { post ->
            if (post.id == videoId) post.copy(commentCount = oldCommentCount) else post
        }
        _uiState.value = latestState.copy(posts = rolledBackPosts)
    }

    private fun loadVideoComments(videoId: String) {
        viewModelScope.launch {
            when (val result = postRepository.getVideoComments(videoId, page = 0, size = VIDEO_COMMENT_PAGE_SIZE)) {
                is ApiResult.Success -> {
                    _commentsSheetState.value = _commentsSheetState.value.copy(
                        isLoading = false,
                        comments = result.data,
                        errorMessage = null
                    )
                }

                is ApiResult.Error -> {
                    _commentsSheetState.value = _commentsSheetState.value.copy(
                        isLoading = false,
                        comments = emptyList(),
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}

data class VideoCommentsSheetState(
    val isVisible: Boolean = false,
    val videoId: String? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val commentInput: String = "",
    val replyingToComment: Comment? = null,
    val selectedMediaUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)
