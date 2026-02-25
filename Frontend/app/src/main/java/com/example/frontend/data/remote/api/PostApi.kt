package com.example.frontend.data.remote.api

import retrofit2.http.Query
import com.example.frontend.domain.model.Post
import retrofit2.http.GET
import retrofit2.http.Path

interface PostApi {
    @GET(ApiRoutes.NEWS_FEED)
    suspend fun getNewsFeed(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ) : List<Post>

    @GET(ApiRoutes.GET_VIDEO)
    suspend fun getVideo(): List<Post>

    @GET(ApiRoutes.USER_POSTS)
    suspend fun getUserPosts(
        @Path("userId") userId: String,
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Post>
}