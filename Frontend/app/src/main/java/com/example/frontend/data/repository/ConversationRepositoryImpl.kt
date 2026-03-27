package com.example.frontend.data.repository

import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.ConversationApi
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.repository.ConversationRepository
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi
) : ConversationRepository {

    override suspend fun getConversations(): ApiResult<List<Conversation>> {
        return try {
            val response = conversationApi.getMyConversations()
            ApiResult.Success(response)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng. Vui lòng thử lại.", throwable = e)
        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
            } else {
                "Lỗi máy chủ ($code). Vui lòng thử lại sau."
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu không đúng định dạng.", throwable = e)
        } catch (e: Exception) {
            Log.e("ConversationRepo", "getConversations error", e)
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }

    override suspend fun searchConversations(keyword: String): ApiResult<List<Conversation>> {
        return try {
            val response = conversationApi.searchConversations(keyword)
            ApiResult.Success(response)
        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
            } else {
                "Lỗi máy chủ ($code). Vui lòng thử lại sau."
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu không đúng định dạng.", throwable = e)
        } catch (e: Exception) {
            Log.e("ConversationRepo", "getConversations error", e)
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }
}
