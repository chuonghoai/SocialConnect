package com.example.frontend.domain.usecase.NotificationUseCase

import com.example.frontend.domain.repository.NotiRepository
import javax.inject.Inject

class GetMyNotificationsUseCase @Inject constructor(
    private val repo: NotiRepository
) {
    suspend operator fun invoke(
        limit: Int = 20,
        offset: Int = 0,
        isRead: Boolean? = null
    ) = repo.getMyNotifications(limit, offset, isRead)
}