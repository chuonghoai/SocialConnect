package com.example.frontend.data.repository

import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.mapper.toDomainPost
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

            val posts = postApi.getNewsFeed(lastPostId = afterId).map { it.toDomainPost() }
            posts.forEach { post ->
                Log.d(
                    TAG,
                    "newsfeed post=${post.id}, mediaCount=${post.media.orEmpty().size}, cdnUrl=${post.cdnUrl}"
                )
            }

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
            ApiResult.Error(message = "Loi mang: Vui long kiem tra lai ket noi Internet.", throwable = e)

        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phien dang nhap da het han. Vui long dang nhap lai."
            } else {
                "Loi may chu ($code). Vui long thu lai sau."
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Du lieu tu server khong dung dinh dang.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getNewsFeed() unexpected error", e)
            ApiResult.Error(
                message = e.message?.takeIf { it.isNotBlank() } ?: "Da xay ra loi khong xac dinh.",
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

            val posts = postApi.getUserPosts(userId, afterId).map { it.toDomainPost() }

            if (afterId == null && !isRefresh) postDao.clearUserPosts(userId)
            postDao.insertPosts(posts.map { it.toEntity() })
            ApiResult.Success(posts)

        } catch (e: IOException) {
            if (!isRefresh && afterId == null) {
                val localPosts = postDao.getPostsByUserId(userId).map { it.toDomain() }
                if (localPosts.isNotEmpty()) return ApiResult.Success(localPosts)
            }
            ApiResult.Error(message = "Loi mang", throwable = e)
        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phien dang nhap da het han. Vui long dang nhap lai."
            } else {
                "Loi may chu"
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Du lieu tu server khong dung dinh dang.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getUserPosts() unexpected error", e)
            ApiResult.Error(message = e.message?.takeIf { it.isNotBlank() } ?: "Loi khong xac dinh", throwable = e)
        }
    }

    override suspend fun getVideos(afterId: String?, isRefresh: Boolean): ApiResult<List<Post>> {
        return try {
            if (isRefresh && afterId == null) {
                postDao.clearCachedVideos()
            }

            val videos = postApi.getVideo(lastPostId = afterId).map { it.toDomainPost() }

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
            ApiResult.Error(message = "Loi mang: Vui long kiem tra lai ket noi Internet.", throwable = e)

        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phien dang nhap da het han. Vui long dang nhap lai."
            } else {
                "Loi may chu ($code). Vui long thu lai sau."
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: JsonParseException) {
            ApiResult.Error(message = "Du lieu tu server khong dung dinh dang.", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "getVideos() unexpected error", e)
            ApiResult.Error(
                message = e.message?.takeIf { it.isNotBlank() } ?: "Da xay ra loi khong xac dinh.",
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
            ApiResult.Error(message = "Loi ket noi mang", throwable = e)
        } catch (e: HttpException) {
            val code = e.code()
            val message = if (code == 401 || code == 403) {
                "Phien dang nhap da het han. Vui long dang nhap lai."
            } else {
                "Loi may chu ($code)"
            }
            ApiResult.Error(code = code, message = message, throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Loi khong xac dinh", throwable = e)
        }
    }

    override suspend fun createPost(content: String, visibility: String, mediaIds: List<String>?): ApiResult<String> {
        return try {
            val normalizedVisibility = normalizeVisibility(visibility)
            val requestBody = CreatePostRequest(
                content = content,
                visibility = normalizedVisibility,
                mediaId = mediaIds
            )

            val response = postApi.createPost(requestBody)
            val returnedPostId = response["postId"] ?: ""

            ApiResult.Success(returnedPostId)
        } catch (e: HttpException) {
            val serverBody = e.response()?.errorBody()?.string()?.take(500).orEmpty()
            val message = if (serverBody.isNotBlank()) {
                "Loi Server (${e.code()}): $serverBody"
            } else {
                "Loi Server (${e.code()}): ${e.message()}"
            }
            ApiResult.Error(message = message, throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Loi dang bai: ${e.message}", throwable = e)
        }
    }

    private fun normalizeVisibility(raw: String): String {
        return when (raw.trim().lowercase()) {
            "cong khai", "công khai", "public" -> "PUBLIC"
            "ban be", "bạn bè", "friends", "friend" -> "FRIENDS"
            "rieng tu", "riêng tư", "private" -> "PRIVATE"
            else -> raw.trim().uppercase()
        }
    }
}
