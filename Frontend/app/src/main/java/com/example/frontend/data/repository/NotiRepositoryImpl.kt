package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.NotificationApi
import com.example.frontend.domain.model.NotificationItem
import com.example.frontend.domain.model.NotificationsPage
import com.example.frontend.domain.repository.NotiRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotiRepositoryImpl @Inject constructor(
    private val api: NotificationApi
) : NotiRepository {

    override suspend fun getMyNotifications(
        limit: Int,
        offset: Int,
        isRead: Boolean?
    ): ApiResult<NotificationsPage> {
        return try {
            val response = api.getMyNotifications(limit = limit, offset = offset, isRead = isRead)
            val mapped = NotificationsPage(
                total = response.total,
                items = response.items.map { dto ->
                    NotificationItem(
                        id = dto.id,
                        type = dto.type,
                        sourceType = dto.sourceType,
                        sourceId = dto.sourceId,
                        content = dto.content,
                        url = dto.url,
                        metadata = dto.metadata,
                        isRead = dto.isRead,
                        createAt = dto.createAt
                    )
                }
            )
            ApiResult.Success(mapped)
        } catch (e: IOException) {
            ApiResult.Error(message = "Loi ket noi mang", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Loi may chu (${e.code()})", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Khong the tai thong bao", throwable = e)
        }
    }

    override suspend fun markAsRead(notificationId: Int): ApiResult<Unit> {
        return try {
            val response = api.markAsRead(notificationId)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(message = response.message.ifBlank { "Khong the danh dau da doc" })
            }
        } catch (e: IOException) {
            ApiResult.Error(message = "Loi ket noi mang", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Loi may chu (${e.code()})", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Khong the danh dau thong bao", throwable = e)
        }
    }
}