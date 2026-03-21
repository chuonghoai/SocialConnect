package com.example.frontend.data.remote.api

import GetMessagesResponse
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
}
