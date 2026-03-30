package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.dto.CreateConversationResponse
import com.example.frontend.domain.model.Conversation

interface ConversationRepository {
    suspend fun getConversations(): ApiResult<List<Conversation>>
    suspend fun searchConversations(keyword: String): ApiResult<List<Conversation>>
    suspend fun createConversation(participantIds: List<String>): ApiResult<CreateConversationResponse>
}