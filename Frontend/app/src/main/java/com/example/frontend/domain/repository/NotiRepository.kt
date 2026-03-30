package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.NotificationsPage

interface NotiRepository {
    suspend fun getMyNotifications(
        limit: Int = 20,
        offset: Int = 0,
        isRead: Boolean? = null
    ): ApiResult<NotificationsPage>

    suspend fun markAsRead(notificationId: Int): ApiResult<Unit>
    suspend fun getUnseenNotificationsCount(): ApiResult<Int>
    suspend fun markAllAsSeen(): ApiResult<Unit>
}