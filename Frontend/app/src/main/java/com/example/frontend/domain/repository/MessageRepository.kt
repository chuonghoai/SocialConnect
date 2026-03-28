package com.example.frontend.domain.repository

import GetMessagesResponse
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.MediaHistoryItem

interface MessageRepository {
    suspend fun getMessages(conversationId: String, page: Int = 1, limit: Int = 20): ApiResult<GetMessagesResponse>
    suspend fun getConversationMedias(conversationId: String, page: Int, limit: Int): ApiResult<List<MediaHistoryItem>>
}
