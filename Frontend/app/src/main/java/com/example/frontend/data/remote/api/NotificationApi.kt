package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.MarkAsReadResponseDto
import com.example.frontend.data.remote.dto.NotificationsResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET(ApiRoutes.GET_NOTIFICATIONS_ME)
    suspend fun getMyNotifications(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("isRead") isRead: Boolean? = null
    ): NotificationsResponseDto

    @PATCH(ApiRoutes.MARK_NOTIFICATION_READ)
    suspend fun markAsRead(
        @Path("notificationId") notificationId: Int
    ): MarkAsReadResponseDto
}