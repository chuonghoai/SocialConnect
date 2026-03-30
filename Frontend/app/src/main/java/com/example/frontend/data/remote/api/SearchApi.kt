package com.example.frontend.data.remote.api

import com.example.frontend.domain.model.SearchRequest
import com.example.frontend.domain.model.SearchResult
import retrofit2.http.Body
import retrofit2.http.POST

interface SearchApi {
    @POST(ApiRoutes.SEARCH)
    suspend fun search(@Body request: SearchRequest): SearchResult
}
