package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.data.remote.dto.CreateCommentRequest
import com.example.frontend.data.remote.dto.CreatePostRequest
import com.example.frontend.data.remote.dto.ReportPostRequest
import retrofit2.http.Query
import retrofit2.Response
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

    @POST(ApiRoutes.SHARE_POST)
    suspend fun sharePost(@Path("postId") postId: String): Map<String, String>

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

    @GET(ApiRoutes.SAVED_POSTS)
    suspend fun getSavedPosts(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Map<String, Any?>>

    @POST(ApiRoutes.LIKE_POST)
    suspend fun likePost(@Path("postId") postId: String)

    @POST(ApiRoutes.SAVE_POST)
    suspend fun savePost(@Path("postId") postId: String): Map<String, Boolean>

    @POST(ApiRoutes.REPORT_POST)
    suspend fun reportPost(
        @Path("postId") postId: String,
        @Body request: ReportPostRequest
    ): Response<Unit>

    @POST(ApiRoutes.CREATE_POST)
    suspend fun createPost(@Body request: CreatePostRequest): Map<String, String>

    @GET(ApiRoutes.GET_POST_COMMENTS)
    suspend fun getPostComments(
        @Path("postId") postId: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): List<CommentResponseDto>

    @POST(ApiRoutes.CREATE_POST_COMMENT)
    suspend fun createComment(
        @Path("postId") postId: String,
        @Body request: CreateCommentRequest
    ): Response<Unit>
}
