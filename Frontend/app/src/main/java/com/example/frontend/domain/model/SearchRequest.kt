package com.example.frontend.domain.model

/**
 * Contract DTO 4.1 – SearchRequestDto (bản trung bình đã thống nhất).
 *
 * keyword  : từ khoá cần tìm
 * scope    : phạm vi tìm kiếm ("ALL" | "USER" | "POST")
 * limitUsers: số user tối đa trả về (default 5)
 * limitPosts: số post tối đa trả về (default 10)
 */
data class SearchRequest(
    val keyword: String,
    val scope: String = "ALL",
    val limitUsers: Int = 5,
    val limitPosts: Int = 10
)
