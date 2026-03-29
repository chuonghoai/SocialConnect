package com.example.frontend.data.repository

import GetMessagesResponse
import MessageContextResponse
import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.MessageApi
import com.example.frontend.domain.model.MediaHistoryItem
import com.example.frontend.domain.repository.MessageRepository
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi
) : MessageRepository {

    override suspend fun getMessages(conversationId: String, page: Int, limit: Int): ApiResult<GetMessagesResponse> {
        return try {
            val response = messageApi.getMessages(conversationId, page, limit)
            ApiResult.Success(response)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng.", throwable = e)
        } catch (e: HttpException) {
            val code = e.code()
            ApiResult.Error(code = code, message = "Lỗi máy chủ ($code).", throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu không đúng định dạng.", throwable = e)
        } catch (e: Exception) {
            Log.e("MessageRepo", "getMessages error", e)
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }

    override suspend fun getConversationMedias(conversationId: String, page: Int, limit: Int): ApiResult<List<MediaHistoryItem>> {
        return try {
            val response = messageApi.getConversationMedias(conversationId, page, limit)
            ApiResult.Success(response.medias.map { it.toDomain() })
        } catch (e: retrofit2.HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ khi lấy ảnh/video", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định", throwable = e)
        }
    }

    override suspend fun getMessageContext(conversationId: String, messageId: String, limit: Int): ApiResult<MessageContextResponse> {
        return try {
            val response = messageApi.getMessageContext(conversationId, messageId, limit)
            ApiResult.Success(response)
        } catch (e: java.io.IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng.", throwable = e)
        } catch (e: retrofit2.HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ khi lấy context.", throwable = e)
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "getMessageContext error", e)
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }
}
