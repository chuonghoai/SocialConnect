package com.example.frontend.data.remote.api

import retrofit2.http.Query
import com.example.frontend.data.remote.dto.PostResponseDto
import retrofit2.http.GET

interface PostApi {
    @GET(ApiRoutes.NEWS_FEED)
    suspend fun getNewsFeed(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ) : List<PostResponseDto>
}