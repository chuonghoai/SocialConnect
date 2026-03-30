package com.example.frontend.domain.usecase.ConversationUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.repository.ConversationRepository
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend operator fun invoke(): ApiResult<List<Conversation>> {
        return repository.getConversations()
    }
}
