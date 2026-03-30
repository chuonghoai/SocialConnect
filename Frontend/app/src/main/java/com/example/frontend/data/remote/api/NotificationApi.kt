package com.example.frontend.data.remote.api

import com.example.frontend.data.remote.dto.MarkAsReadResponseDto
import com.example.frontend.data.remote.dto.NotificationsResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

data class UnseenCountResponseDto(val unseenCount: Int)

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

    @GET(ApiRoutes.GET_UNSEEN_NOTIFICATIONS_COUNT)
    suspend fun getUnseenNotificationsCount(): UnseenCountResponseDto

    @PATCH(ApiRoutes.MARK_ALL_NOTIFICATIONS_READ)
    suspend fun markAllAsSeen(): MarkAsReadResponseDto
}