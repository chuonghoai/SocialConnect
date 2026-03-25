package com.example.frontend.data.repository

import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.local.dao.SearchHistoryDao
import com.example.frontend.data.local.entity.SearchHistoryEntity
import com.example.frontend.data.remote.api.SearchApi
import com.example.frontend.domain.model.SearchRequest
import com.example.frontend.domain.model.SearchResult
import com.example.frontend.domain.repository.SearchRepository
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchApi: SearchApi,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

    private companion object {
        const val TAG = "SearchRepository"
    }

    override suspend fun search(
        keyword: String,
        scope: String,
        limitUsers: Int,
        limitPosts: Int
    ): ApiResult<SearchResult> {
        return try {
            val request = SearchRequest(
                keyword = keyword,
                scope = scope,
                limitUsers = limitUsers,
                limitPosts = limitPosts
            )
            Log.d(TAG, "search request: keyword=$keyword, scope=$scope, limitUsers=$limitUsers, limitPosts=$limitPosts")
            val result = searchApi.search(request)
            ApiResult.Success(result)
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error while searching", e)
            val serverBody = e.response()?.errorBody()?.string()?.take(300).orEmpty()
            val message = when (e.code()) {
                401, 403 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                404 -> "Không tìm thấy API tìm kiếm. Hãy kiểm tra BASE_URL và route backend."
                else -> {
                    if (serverBody.isNotBlank()) {
                        "Lỗi máy chủ (${e.code()}): $serverBody"
                    } else {
                        "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau."
                    }
                }
            }
            ApiResult.Error(
                code = e.code(),
                message = message,
                throwable = e
            )
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout while searching", e)
            ApiResult.Error(
                message = "Không kết nối được tới máy chủ (timeout). Hãy kiểm tra BE đang chạy và BASE_URL.",
                throwable = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error while searching", e)
            ApiResult.Error(
                message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.",
                throwable = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while searching", e)
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
            ApiResult.Error(
                message = "Đã xảy ra lỗi không xác định: $detail",
                throwable = e
            )
        }
    }

    override suspend fun getSearchHistory(): List<String> =
        searchHistoryDao.getAll().map { it.keyword }

    override suspend fun addSearchHistory(keyword: String) {
        searchHistoryDao.insert(
            SearchHistoryEntity(
                keyword = keyword,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteSearchHistory(keyword: String) {
        searchHistoryDao.deleteByKeyword(keyword)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }
}
