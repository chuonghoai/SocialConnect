package com.example.frontend.domain.usecase.FriendUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.FriendRepository
import javax.inject.Inject

class CancelFriendRequestUseCase @Inject constructor(
    private val repository: FriendRepository
) {
    suspend operator fun invoke(friendId: String): ApiResult<Unit> {
        return repository.cancelFriendRequest(friendId)
    }
}