package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.data.remote.dto.PostResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PostApi {
    @GET(ApiRoutes.NEWS_FEED)
    suspend fun getNewsFeed(): List<PostResponseDto>

    @GET(ApiRoutes.GET_POST_COMMENTS)
    suspend fun getPostComments(
        @Path("postId") postId: String
    ): List<CommentResponseDto>
}