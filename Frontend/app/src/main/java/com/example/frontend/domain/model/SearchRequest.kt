package com.example.frontend.domain.model

data class SearchRequest(
    val keyword: String,
    val scope: String = "ALL",
    val limitUsers: Int = 5,
    val limitPosts: Int = 10
)
