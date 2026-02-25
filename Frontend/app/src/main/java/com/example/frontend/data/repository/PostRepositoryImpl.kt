package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.repository.PostRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postApi: PostApi,
    private val postDao: PostDao
) : PostRepository {

    override suspend fun getNewsFeed(afterId: String?, isRefresh: Boolean): ApiResult<List<Post>> {
        return try {
            if (isRefresh && afterId == null) {
                postDao.clearAllPosts()
            }

            val posts = postApi.getNewsFeed(lastPostId = afterId)

            if (afterId == null && !isRefresh) {
                postDao.clearAllPosts()
            }
            postDao.insertPosts(posts.map { it.toEntity() })

            ApiResult.Success(posts)

        } catch (e: IOException) {
            if (!isRefresh && afterId == null) {
                val localPosts = postDao.getAllPosts().map { it.toDomain() }
                if (localPosts.isNotEmpty()) {
                    return ApiResult.Success(localPosts)
                }
            }
            ApiResult.Error(message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.", throwable = e)

        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau.", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }

    override suspend fun getUserPosts(
        userId: String,
        afterId: String?,
        isRefresh: Boolean
    ): ApiResult<List<Post>> {
        return try {
            if (isRefresh && afterId == null) postDao.clearUserPosts(userId)

            val posts = postApi.getUserPosts(userId, afterId)

            if (afterId == null && !isRefresh) postDao.clearUserPosts(userId)
            postDao.insertPosts(posts.map { it.toEntity() })
            ApiResult.Success(posts)

        } catch (e: IOException) {
            if (!isRefresh && afterId == null) {
                val localPosts = postDao.getPostsByUserId(userId).map { it.toDomain() }
                if (localPosts.isNotEmpty()) return ApiResult.Success(localPosts)
            }
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Lỗi không xác định", throwable = e)
        }
    }
}