package com.example.frontend.domain.usecase.MessageUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.MediaHistoryItem
import com.example.frontend.domain.repository.MessageRepository
import javax.inject.Inject

class GetConversationMediasUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, page: Int = 1, limit: Int = 30): ApiResult<List<MediaHistoryItem>> {
        return messageRepository.getConversationMedias(conversationId, page, limit)
    }
}