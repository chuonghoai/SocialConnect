package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Post

interface PostRepository {
    suspend fun getNewsFeed(): ApiResult<List<Post>>
}