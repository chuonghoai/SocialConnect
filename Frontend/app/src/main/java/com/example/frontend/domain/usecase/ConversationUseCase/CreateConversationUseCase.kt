package com.example.frontend.domain.usecase.ConversationUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.dto.CreateConversationResponse
import com.example.frontend.domain.repository.ConversationRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(partnerId: String): ApiResult<CreateConversationResponse> {
        return conversationRepository.createConversation(listOf(partnerId))
    }
}