package com.example.frontend.domain.model

data class SearchResult(
    val keyword: String,
    val users: List<SearchUserItem>,
    val posts: List<Post>,
    val totalUsers: Int = 0,
    val totalPosts: Int = 0
)
