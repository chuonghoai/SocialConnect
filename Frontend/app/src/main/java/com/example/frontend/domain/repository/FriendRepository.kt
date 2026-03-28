package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRequestItem
import com.example.frontend.domain.model.FriendRecipient

interface FriendRepository {
    suspend fun getShareFriends(currentUserId: String): ApiResult<List<FriendRecipient>>
    suspend fun addFriend(friendId: String): ApiResult<Unit>
    suspend fun getFriendRequests(): ApiResult<List<FriendRequestItem>>
    suspend fun acceptFriendRequest(friendId: String): ApiResult<Unit>
    suspend fun rejectFriendRequest(friendId: String): ApiResult<Unit>
    suspend fun getMyFriends(): ApiResult<List<FriendRecipient>>
    suspend fun getUserFriends(userId: String): ApiResult<List<FriendRecipient>>
    
}
