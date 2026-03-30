package com.example.frontend.domain.model

data class NotificationItem(
    val id: Int,
    val type: String,
    val sourceType: String,
    val sourceId: String,
    val content: String,
    val url: String?,
    val metadata: Map<String, Any?>?,
    val isRead: Boolean,
    val createAt: String,
    val user: NotificationUser? = null
)

data class NotificationUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

data class NotificationsPage(
    val total: Int,
    val items: List<NotificationItem>
)
