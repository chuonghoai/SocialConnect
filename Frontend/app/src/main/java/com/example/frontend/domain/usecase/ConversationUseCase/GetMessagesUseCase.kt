package com.example.frontend.domain.usecase.ConversationUseCase

import GetMessagesResponse
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.MessageRepository
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, page: Int = 1, limit: Int = 20): ApiResult<GetMessagesResponse> {
        return repository.getMessages(conversationId, page, limit)
    }
}
