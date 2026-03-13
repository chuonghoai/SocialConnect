package com.example.frontend.presentation.screen.postdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Comment
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
    private val postApi: PostApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    // ── Load post + comments ───────────────────────────────────────────────────
    fun loadPostDetail() {
        val post = postDetailStore.selectedPost ?: return
        _uiState.update {
            it.copy(
                post = post,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isLoadingComments = true,
                commentsError = false
            )
        }
        viewModelScope.launch {
            try {
                val comments = postApi.getPostComments(post.id).map { it.toDomain() }
                Log.d("PostDetail", "Loaded ${comments.size} comments for postId=${post.id}")
                _uiState.update {
                    it.copy(
                        comments = comments,
                        isLoadingComments = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PostDetail", "Failed to load comments for postId=${post.id}", e)
                _uiState.update { it.copy(isLoadingComments = false, commentsError = true) }
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

    fun submitComment() {
        val text = _uiState.value.commentInput.trim()
        if (text.isBlank()) return
        val newComment = Comment(
            id = "c_${System.currentTimeMillis()}",
            userId = "me",
            displayName = "Bạn",
            avatarUrl = "",
            content = text,
            createdAt = "Vừa xong",
            likeCount = 0
        )
        _uiState.update {
            it.copy(
                comments = listOf(newComment) + it.comments,
                commentInput = "",
                commentCount = it.commentCount + 1
            )
        }
    }
}
