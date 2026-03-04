package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Post

interface PostRepository {
    suspend fun getNewsFeed(
        afterId: String? = null,
        isRefresh: Boolean = false
    ): ApiResult<List<Post>>

    suspend fun getUserPosts(
        userId: String,
        afterId: String? = null,
        isRefresh: Boolean = false
    ): ApiResult<List<Post>>

    suspend fun getVideos(
        afterId: String? = null,
        isRefresh: Boolean = false
    ): ApiResult<List<Post>>

    suspend fun likePost(postId: String, isLiked: Boolean, likeCount: Int): ApiResult<Unit>
}