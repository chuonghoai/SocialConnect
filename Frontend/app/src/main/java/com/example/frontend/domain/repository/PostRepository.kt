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

    suspend fun getSavedPosts(
        afterId: String? = null,
        isRefresh: Boolean = false
    ): ApiResult<List<Post>>

    suspend fun getVideos(
        afterId: String? = null,
        isRefresh: Boolean = false
    ): ApiResult<List<Post>>

    suspend fun likePost(postId: String, isLiked: Boolean, likeCount: Int): ApiResult<Unit>

    suspend fun savePost(postId: String): ApiResult<Boolean>

    suspend fun sharePost(postId: String): ApiResult<String>

    suspend fun createPost(content: String, visibility: String, mediaId: List<String>?): ApiResult<String>

    suspend fun updatePost(postId: String, content: String? = null, visibility: String? = null): ApiResult<Unit>

    suspend fun deletePost(postId: String): ApiResult<Unit>
}
