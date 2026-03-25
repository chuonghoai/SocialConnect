package com.example.frontend.domain.repository

import GetMessagesResponse
import com.example.frontend.core.network.ApiResult

interface MessageRepository {
    suspend fun getMessages(conversationId: String, page: Int = 1, limit: Int = 20): ApiResult<GetMessagesResponse>
}
