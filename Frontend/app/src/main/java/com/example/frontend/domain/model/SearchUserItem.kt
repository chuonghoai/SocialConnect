package com.example.frontend.domain.model

/**
 * Contract DTO 4.3 – User item trả về từ search endpoint.
 * Mapping 1-1 với SearchUserItemDto từ backend.
 */
data class SearchUserItem(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val isFriend: Boolean
)
