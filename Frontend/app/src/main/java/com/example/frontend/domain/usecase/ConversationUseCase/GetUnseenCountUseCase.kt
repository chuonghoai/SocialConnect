package com.example.frontend.domain.usecase.ConversationUseCase

import com.example.frontend.domain.repository.NotiRepository
import javax.inject.Inject

class GetUnseenCountUseCase @Inject constructor(private val repository: NotiRepository) {
    suspend operator fun invoke() = repository.getUnseenNotificationsCount()
}