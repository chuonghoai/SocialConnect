package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
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
    private val postApi: PostApi
) : PostRepository {
    override suspend fun getNewsFeed(): ApiResult<List<Post>> {
        return try {
            val dtoList = postApi.getNewsFeed()
            ApiResult.Success(dtoList.map { it.toDomain() })
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = e.message(), throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Network error", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }
}
