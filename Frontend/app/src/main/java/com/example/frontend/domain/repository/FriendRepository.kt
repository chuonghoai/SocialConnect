package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRecipient

interface FriendRepository {
    suspend fun getShareFriends(currentUserId: String): ApiResult<List<FriendRecipient>>
}
