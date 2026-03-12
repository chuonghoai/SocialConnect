package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.SearchResult

interface SearchRepository {
    suspend fun search(
        keyword: String,
        scope: String = "ALL",
        limitUsers: Int = 5,
        limitPosts: Int = 10
    ): ApiResult<SearchResult>

    suspend fun getSearchHistory(): List<String>
    suspend fun addSearchHistory(keyword: String)
    suspend fun deleteSearchHistory(keyword: String)
    suspend fun clearSearchHistory()
}
