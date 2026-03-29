package com.example.frontend.domain.usecase.ConversationUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.MessageRepository
import MessageContextResponse
import javax.inject.Inject

class GetMessageContextUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, messageId: String, limit: Int = 50): ApiResult<MessageContextResponse> {
        return repository.getMessageContext(conversationId, messageId, limit)
    }
}