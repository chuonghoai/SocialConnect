package com.example.frontend.data.repository

import ChangePasswordRequest
import UpdateProfileRequest
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.datastore.TokenDataStore
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.dao.UserDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.AuthApi
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore,
    private val userDao: UserDao,
    private val postDao: PostDao
) : AuthRepository {

    private val gson = Gson()

    override suspend fun getMe(isRefresh: Boolean): ApiResult<User> {
        return try {
            if (isRefresh) {
                userDao.clearUser()
            }

            val user = authApi.me()

            if (!isRefresh) {
                userDao.clearUser()
            }
            userDao.insertUser(user.toEntity())
            ApiResult.Success(user)

        } catch (e: IOException) {
            if (!isRefresh) {
                val localUser = userDao.getUser()
                if (localUser != null) {
                    return ApiResult.Success(localUser.toDomain())
                }
            }
            ApiResult.Error(message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.", throwable = e)

        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau.", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Đã xảy ra lỗi không xác định.", throwable = e)
        }
    }

    override suspend fun getUserProfile(userId: String): ApiResult<User> {
        return try {
            val user = authApi.getUserProfile(userId)
            ApiResult.Success(user)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = "Không thể tải thông tin người dùng (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng: Vui lòng kiểm tra kết nối.", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Không thể tải thông tin người dùng", throwable = e)
        }
    }

    override suspend fun login(username: String, password: String): ApiResult<Unit> {
        return try {
            val body = mapOf("username" to username, "password" to password)
            val res = authApi.login(body)

            tokenDataStore.saveAccessToken(res.accessToken)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string()
            val msg = try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val errorMap: Map<String, String>? = raw?.let { gson.fromJson(it, type) }
                errorMap?.get("message")
            } catch (_: Exception) { null }

            ApiResult.Error(
                code = e.code(),
                message = msg ?: when (e.code()) {
                    401 -> "Sai tài khoản hoặc mật khẩu"
                    else -> "Login failed (${e.code()})"
                },
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Ignore server logout errors (401/timeout...) and always clear local session.
        } finally {
            tokenDataStore.clear()
            userDao.clearUser()
            postDao.clearAllPosts()
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        mailOtp: String
    ): ApiResult<Unit> {
        return try {
            val body = mapOf("email" to email, "password" to password, "mailOtp" to mailOtp)
            authApi.register(body)

            tokenDataStore.clear()
            userDao.clearUser()
            postDao.clearAllPosts()

            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Register failed: ${e.message}", throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Network error", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun sendOtp(email: String, type: String): ApiResult<Unit> {
        return try {
            val body = mapOf("email" to email, "type" to type)
            authApi.sendOtp(body)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(message = "send OTP failed: ${e.message}", throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Network error", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun verifyForgotPasswordOtp(email: String, otp: String): ApiResult<Unit> {
        return try {
            val body = mapOf("email" to email, "otp" to otp)
            authApi.verifyForgotPasswordOtp(body)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Xác thực OTP thất bại: ${e.message}", throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun resetPassword(email: String, newPassword: String): ApiResult<Unit> {
        return try {
            val body = mapOf(
                "email" to email,
                "newPassword" to newPassword,
                "password" to newPassword
            )
            authApi.resetPassword(body)

            // Sau reset password thành công, bắt buộc đăng nhập lại
            tokenDataStore.clear()
            userDao.clearUser()
            postDao.clearAllPosts()

            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Đặt lại mật khẩu thất bại: ${e.message}", throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun updateProfile(displayName: String, dob: String, email: String, avatar: String?): ApiResult<User> {
        return try {
            val response = authApi.updateProfile(UpdateProfileRequest(displayName, dob, email, avatar))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(message = "Lỗi cập nhật hồ sơ")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> {
        return try {
            val response = authApi.changePassword(ChangePasswordRequest(oldPassword, newPassword))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(message = "Lỗi đổi mật khẩu")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Lỗi kết nối")
        }
    }
}
