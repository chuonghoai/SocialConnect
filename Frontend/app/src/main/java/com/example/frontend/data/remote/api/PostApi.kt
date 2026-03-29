package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.data.remote.dto.CreateCommentRequest
import com.example.frontend.data.remote.dto.CreatePostRequest
import com.example.frontend.data.remote.dto.ReportPostRequest
import com.example.frontend.data.remote.dto.SharePostRequest
import com.example.frontend.data.remote.dto.UpdatePostRequest
import com.example.frontend.domain.model.Post
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PostApi {
    @GET(ApiRoutes.NEWS_FEED)
    suspend fun getNewsFeed(
        @Query("after") lastPostId: String? = null,
        @Query("limit") limit: Int = 10
    ) : List<Map<String, Any?>>

    @POST(ApiRoutes.SHARE_POST)
    suspend fun sharePost(
        @Path("postId") postId: String,
        @Body request: SharePostRequest
    ): Map<String, String>

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

    @GET(ApiRoutes.GET_POST_BY_ID)
    suspend fun getPostById(@Path("id") postId: String): Map<String, Any?>

    @POST(ApiRoutes.LIKE_POST)
    suspend fun likePost(@Path("postId") postId: String)

    @POST(ApiRoutes.LIKE_VIDEO)
    suspend fun likeVideo(@Path("videoId") videoId: String)

    @POST(ApiRoutes.SAVE_POST)
    suspend fun savePost(@Path("postId") postId: String): Map<String, Boolean>

    @POST(ApiRoutes.REPORT_POST)
    suspend fun reportPost(
        @Path("postId") postId: String,
        @Body request: ReportPostRequest
    ): Response<Unit>

    @POST(ApiRoutes.SAVE_VIDEO)
    suspend fun saveVideo(@Path("videoId") videoId: String): Map<String, Boolean>

    @POST(ApiRoutes.SHARE_VIDEO)
    suspend fun shareVideo(@Path("videoId") videoId: String): Map<String, String>

    @POST(ApiRoutes.CREATE_POST)
    suspend fun createPost(@Body request: CreatePostRequest): Map<String, String>

    @PATCH(ApiRoutes.UPDATE_POST)
    suspend fun updatePost(
        @Path("postId") postId: String,
        @Body request: UpdatePostRequest
    ): Response<Unit>

    @DELETE(ApiRoutes.DELETE_POST)
    suspend fun deletePost(@Path("postId") postId: String): Response<Unit>

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

    @POST(ApiRoutes.CREATE_VIDEO_COMMENT)
    suspend fun createVideoComment(
        @Path("videoId") videoId: String,
        @Body request: CreateCommentRequest
    ): Response<Unit>

}
