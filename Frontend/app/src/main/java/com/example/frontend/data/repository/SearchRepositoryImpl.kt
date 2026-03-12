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
            ApiResult.Error(
                code = e.code(),
                message = "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau.",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(
                message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = "Đã xảy ra lỗi không xác định.",
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
