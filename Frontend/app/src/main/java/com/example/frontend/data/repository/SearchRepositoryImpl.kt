package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.local.dao.SearchHistoryDao
import com.example.frontend.data.local.entity.SearchHistoryEntity
import com.example.frontend.data.remote.api.SearchApi
import com.example.frontend.domain.model.SearchRequest
import com.example.frontend.domain.model.SearchResult
import com.example.frontend.domain.repository.SearchRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchApi: SearchApi,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

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
            val result = searchApi.search(request)
            ApiResult.Success(result)
        } catch (e: HttpException) {
            val backendMessage = extractBackendMessage(e.response()?.errorBody()?.string())
            ApiResult.Error(
                code = e.code(),
                message = when (e.code()) {
                    400 -> backendMessage ?: "Yêu cầu tìm kiếm không hợp lệ."
                    401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                    403 -> "Bạn không có quyền thực hiện tìm kiếm này."
                    else -> backendMessage ?: "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau."
                },
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = e.message?.takeIf { it.isNotBlank() }?.let {
                    "Không thể tìm kiếm: $it"
                } ?: "Không thể tìm kiếm lúc này. Vui lòng thử lại.",
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

    private fun extractBackendMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
