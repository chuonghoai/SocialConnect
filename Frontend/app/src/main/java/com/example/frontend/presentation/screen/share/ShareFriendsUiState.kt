package com.example.frontend.presentation.screen.share

import com.example.frontend.domain.model.FriendRecipient

data class ShareFriendsUiState(
    val friends: List<FriendRecipient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
