package com.example.frontend.domain.model

/**
 * Contract DTO 4.2 – SearchResponseDto.
 * Bọc cả users lẫn posts từ một search call.
 * Posts reuse Post domain model (tương thích với newsfeed/video/profile).
 */
data class SearchResult(
    val keyword: String,
    val users: List<SearchUserItem>,
    val posts: List<Post>,
    val totalUsers: Int = 0,
    val totalPosts: Int = 0
)
