package com.example.frontend.presentation.screen.postdetail

import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.model.Post

data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentsError: Boolean = false,
    val commentInput: String = "",
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)
