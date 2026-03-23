package com.example.frontend.domain.usecase.FriendUseCase

import com.example.frontend.domain.repository.FriendRepository
import javax.inject.Inject

class GetShareFriendsUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend operator fun invoke(currentUserId: String) =
        friendRepository.getShareFriends(currentUserId)
}
