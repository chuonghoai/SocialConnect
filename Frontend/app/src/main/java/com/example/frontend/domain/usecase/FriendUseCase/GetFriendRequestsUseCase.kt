package com.example.frontend.domain.usecase.FriendUseCase

import com.example.frontend.domain.repository.FriendRepository
import javax.inject.Inject

class GetFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend operator fun invoke() = friendRepository.getFriendRequests()
}
