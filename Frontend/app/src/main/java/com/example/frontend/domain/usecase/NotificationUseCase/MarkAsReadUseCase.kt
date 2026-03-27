package com.example.frontend.domain.usecase.NotificationUseCase

import com.example.frontend.domain.repository.NotiRepository
import javax.inject.Inject

class MarkAsReadUseCase @Inject constructor(
    private val repo: NotiRepository
) {
    suspend operator fun invoke(notificationId: Int) =
        repo.markAsRead(notificationId)
}