package com.example.frontend.presentation.screen.postdetail

import android.net.Uri
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.model.Post

data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val hasMoreComments: Boolean = true,
    val commentsError: Boolean = false,
    val commentInput: String = "",
    val isSendingComment: Boolean = false,
    val selectedMediaUri: Uri? = null,
    val replyingToComment: Comment? = null,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)
