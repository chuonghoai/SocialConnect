package com.example.frontend.data.remote.api

import GetMessagesResponse
import MessageContextResponse
import com.example.frontend.data.remote.dto.MediaHistoryResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {
    @GET(ApiRoutes.MESSAGES)
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): GetMessagesResponse

    @GET("${ApiRoutes.MESSAGES}/medias")
    suspend fun getConversationMedias(
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): MediaHistoryResponseDto

    @GET("${ApiRoutes.MESSAGES}/{conversationId}/context/{messageId}")
    suspend fun getMessageContext(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
        @Query("limit") limit: Int = 50
    ): MessageContextResponse
}
