package com.example.frontend.domain.usecase.FriendUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.FriendRepository
import javax.inject.Inject

class DeleteFriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend operator fun invoke(friendId: String): ApiResult<Unit> {
        return friendRepository.deleteFriend(friendId)
    }
}