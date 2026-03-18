package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.data.remote.dto.CreatePostRequest
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PostApi {
    @GET(ApiRoutes.NEWS_FEED)
    suspend fun getNewsFeed(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ) : List<Map<String, Any?>>

    @GET(ApiRoutes.GET_VIDEO)
    suspend fun getVideo(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Map<String, Any?>>

    @GET(ApiRoutes.USER_POSTS)
    suspend fun getUserPosts(
        @Path("userId") userId: String,
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Map<String, Any?>>

    @POST(ApiRoutes.LIKE_POST)
    suspend fun likePost(@Path("postId") postId: String)

    @POST(ApiRoutes.CREATE_POST)
    suspend fun createPost(@Body request: CreatePostRequest): Map<String, String>

    @GET(ApiRoutes.GET_POST_COMMENTS)
    suspend fun getPostComments(
        @Path("postId") postId: String
    ): List<CommentResponseDto>
}
