package com.example.frontend.presentation.screen.share

data class SharePostSubmitData(
    val shareText: String,
    val target: String,
    val privacy: String,
    val selectedFriendIds: List<String>,
    val currentUserId: String,
    val postId: String
)
