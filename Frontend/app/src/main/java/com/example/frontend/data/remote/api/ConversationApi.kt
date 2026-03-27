package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.ConversationDto
import com.example.frontend.domain.model.Conversation
import retrofit2.http.GET
import retrofit2.http.Path

interface ConversationApi {
    @GET(ApiRoutes.CONVERSATIONS)
    suspend fun getMyConversations(): List<Conversation>

    @GET(ApiRoutes.CONVERSATIONS_SEARCH)
    suspend fun searchConversations(@Path("keyword") keyword: String): List<Conversation>
}
