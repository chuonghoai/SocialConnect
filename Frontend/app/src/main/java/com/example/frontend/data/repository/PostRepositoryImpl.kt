package com.example.frontend.data.repository

import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.data.remote.dto.CreatePostRequest
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.repository.PostRepository
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postApi: PostApi,
    private val postDao: PostDao
) : PostRepository {

    companion object {
        private const val TAG = "PostRepositoryImpl"
    }

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
            ApiResult.Error(message = "Lỗi mạng", throwable = e)

        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu phản hồi từ máy chủ không hợp lệ.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getNewsFeed() unexpected error", e)
            ApiResult.Error(
                message = e.message?.takeIf { it.isNotBlank() } ?: "Đã xảy ra lỗi không xác định.",
                throwable = e
            )
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
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu phản hồi từ máy chủ không hợp lệ.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getUserPosts() unexpected error", e)
            ApiResult.Error(message = e.message?.takeIf { it.isNotBlank() } ?: "Đã xảy ra lỗi", throwable = e)
        }
    }

    override suspend fun getSavedPosts(afterId: String?, isRefresh: Boolean): ApiResult<List<Post>> {
        return try {
            val posts = postApi.getSavedPosts(lastPostId = afterId).map { it.copy(isSaved = true) }
            ApiResult.Success(posts)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu phản hồi từ máy chủ không hợp lệ.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getSavedPosts() unexpected error", e)
            ApiResult.Error(message = e.message?.takeIf { it.isNotBlank() } ?: "Đã xảy ra lỗi", throwable = e)
        }
    }

    override suspend fun getVideos(afterId: String?, isRefresh: Boolean): ApiResult<List<Post>> {
        return try {
            if (isRefresh && afterId == null) {
                postDao.clearCachedVideos()
            }

            val videos = postApi.getVideo(lastPostId = afterId)

            if (afterId == null && !isRefresh) {
                postDao.clearCachedVideos()
            }
            postDao.insertPosts(videos.map { it.toEntity() })

            ApiResult.Success(videos)

        } catch (e: IOException) {
            if (!isRefresh && afterId == null) {
                val localVideos = postDao.getCachedVideos().map { it.toDomain() }
                if (localVideos.isNotEmpty()) {
                    return ApiResult.Success(localVideos)
                }
            }
            ApiResult.Error(message = "Lỗi mạng", throwable = e)

        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Dữ liệu phản hồi từ máy chủ không hợp lệ.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getVideos() unexpected error", e)
            ApiResult.Error(
                message = e.message?.takeIf { it.isNotBlank() } ?: "Đã xảy ra lỗi không xác định.",
                throwable = e
            )
        }
    }

    override suspend fun likePost(postId: String, isLiked: Boolean, likeCount: Int): ApiResult<Unit> {
        return try {
            postApi.likePost(postId)
            postDao.updateLikeStatus(postId, isLiked, likeCount)

            ApiResult.Success(Unit)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Đã xảy ra lỗi", throwable = e)
        }
    }

    override suspend fun savePost(postId: String): ApiResult<Boolean> {
        return try {
            val response = postApi.savePost(postId)
            ApiResult.Success(response["saved"] == true)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Đã xảy ra lỗi", throwable = e)
        }
    }

    override suspend fun sharePost(postId: String): ApiResult<String> {
        return try {
            val response = postApi.sharePost(postId)
            ApiResult.Success(response["postId"].orEmpty())
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()})", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Không thể chia sẻ bài viết", throwable = e)
        }
    }

    override suspend fun createPost(content: String, visibility: String, mediaId: List<String>?): ApiResult<String> {
        return try {
            val requestBody = CreatePostRequest(
                content = content,
                visibility = visibility,
                mediaId = mediaId
            )

            val response = postApi.createPost(requestBody)
            val returnedPostId = response["postId"] ?: ""

            ApiResult.Success(returnedPostId)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Lỗi máy chủ (${e.code()}): ${e.message()}", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Không thể đăng bài: ${e.message}", throwable = e)
        }
    }
}
