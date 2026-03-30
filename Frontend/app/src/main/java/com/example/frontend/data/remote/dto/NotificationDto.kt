package com.example.frontend.data.remote.dto

data class NotificationDto(
    val id: Int,
    val type: String,
    val sourceType: String,
    val sourceId: String,
    val content: String,
    val url: String? = null,
    val metadata: Map<String, Any?>? = null,
    val isRead: Boolean,
    val createAt: String,
    val user: NotificationUserDto? = null
)

data class NotificationUserDto(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

data class NotificationsResponseDto(
    val total: Int,
    val items: List<NotificationDto>
)

data class MarkAsReadResponseDto(
    val success: Boolean,
    val message: String,
    val data: NotificationDto? = null
)
