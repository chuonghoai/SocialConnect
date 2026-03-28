package com.example.frontend.presentation.screen.search

import com.example.frontend.domain.model.SearchResult

/** Scope filter hiển thị trên UI, map sang giá trị backend nhận */
enum class SearchScope(val value: String, val label: String) {
    ALL("ALL", "Tất cả"),
    USER("USER", "Người dùng"),
    POST("POST", "Bài viết")
}

data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.ALL,
    /** Lịch sử tìm kiếm lấy từ Room, mới nhất trước */
    val searchHistory: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val results: SearchResult? = null,
    val error: String? = null,
    /** true sau khi user bấm Search ít nhất một lần */
    val hasSearched: Boolean = false
)
