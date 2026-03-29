package com.example.frontend.domain.usecase.ConversationUseCase

import com.example.frontend.domain.repository.NotiRepository
import jakarta.inject.Inject

class MarkAllAsSeenUseCase @Inject constructor(private val repository: NotiRepository) {
    suspend operator fun invoke() = repository.markAllAsSeen()
}