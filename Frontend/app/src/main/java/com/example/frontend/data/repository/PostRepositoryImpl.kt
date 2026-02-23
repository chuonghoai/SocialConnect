package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.mapper.toDomain
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

    override suspend fun getNewsFeed(afterId: String?): ApiResult<List<Post>> {
        return try {
            val dtoList = postApi.getNewsFeed(lastPostId = afterId)
            val domainPosts = dtoList.map { it.toDomain() }

            if (afterId == null) {
                postDao.clearAllPosts()
            }
            postDao.insertPosts(domainPosts.map { it.toEntity() })

            ApiResult.Success(domainPosts)

        } catch (e: IOException) {
            val localPosts = postDao.getAllPosts().map { it.toDomain() }

            if (localPosts.isNotEmpty()) {
                ApiResult.Success(localPosts)
            } else {
                ApiResult.Error(message = "Network error and no local cache", throwable = e)
            }
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = e.message(), throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }
}