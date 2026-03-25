package com.example.frontend.data.remote.api

import com.example.frontend.domain.model.Conversation
import retrofit2.http.GET

interface ConversationApi {
    @GET(ApiRoutes.CONVERSATIONS)
    suspend fun getMyConversations(): List<Conversation>
}
