package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.ConversationDto
import retrofit2.http.GET

interface ConversationApi {
    @GET(ApiRoutes.CONVERSATIONS)
    suspend fun getMyConversations(): List<ConversationDto>
}
