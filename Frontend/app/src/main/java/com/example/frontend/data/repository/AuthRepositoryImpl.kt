package com.example.frontend.data.repository

import ChangePasswordRequest
import UpdateProfileRequest
import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.datastore.TokenDataStore
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.dao.UserDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.AuthApi
import com.example.frontend.domain.model.AdminUserItem
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

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
            ApiResult.Error(
                message = "Lỗi mạng: Vui lòng kiểm tra lại kết nối Internet.",
                throwable = e
            )
        } catch (e: HttpException) {
            if (!isRefresh && e.code() == 429) {
                val localUser = userDao.getUser()
                if (localUser != null) {
                    return ApiResult.Success(localUser.toDomain())
                }
            }
            ApiResult.Error(
                code = e.code(),
                message = "Lỗi máy chủ (${e.code()}). Vui lòng thử lại sau.",
                throwable = e
            )
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
            val msg = extractHttpErrorMessage(e)
            ApiResult.Error(
                code = e.code(),
                message = msg ?: when (e.code()) {
                    401 -> "Sai tài khoản hoặc mật khẩu"
                    else -> "Đăng nhập thất bại (${e.code()})"
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
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi khi gọi API logout: ${e.message}")
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
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Đăng ký thất bại (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
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
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Gửi OTP thất bại (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
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
            val backendMessage = extractHttpErrorMessage(e)
            ApiResult.Error(
                code = e.code(),
                message = normalizeForgotOtpErrorMessage(e.code(), backendMessage),
                throwable = e
            )
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

            tokenDataStore.clear()
            userDao.clearUser()
            postDao.clearAllPosts()

            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Đặt lại mật khẩu thất bại (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi kết nối mạng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun updateProfile(
        displayName: String,
        dob: String,
        email: String,
        avatar: String?
    ): ApiResult<User> {
        return try {
            val response = authApi.updateProfile(
                UpdateProfileRequest(
                    displayName = displayName.trim().ifEmpty { null },
                    avatarUrl = avatar
                )
            )
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                val backendMessage = response.errorBody()?.string()?.trim().orEmpty()
                ApiResult.Error(
                    code = response.code(),
                    message = backendMessage.ifBlank { "Loi cap nhat ho so (${response.code()})" }
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Loi cap nhat ho so (${e.code()})",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Loi ket noi")
        }
    }

    override suspend fun changePassword(newPassword: String, confirmPassword: String): ApiResult<Unit> {
        return try {
            val response = authApi.changePassword(
                ChangePasswordRequest(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                val backendMessage = response.errorBody()?.string()?.trim().orEmpty()
                ApiResult.Error(message = backendMessage.ifBlank { "Loi doi mat khau" })
            }
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Loi doi mat khau (${e.code()})",
                throwable = e
            )
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Loi ket noi")
        }
    }

    override suspend fun lockUser(userId: String): ApiResult<Unit> {
        return try {
            authApi.lockUser(userId)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Không thể khóa tài khoản (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun unlockUser(userId: String): ApiResult<Unit> {
        return try {
            authApi.unlockUser(userId)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Khong the mo khoa tai khoan (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Loi mang", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun deleteUser(userId: String): ApiResult<Unit> {
        return try {
            authApi.deleteUser(userId)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "Không thể xóa tài khoản (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lỗi mạng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun getAdminUsers(limit: Int, offset: Int): ApiResult<List<AdminUserItem>> {
        return try {
            val users = authApi.getAdminUsers(limit = limit, offset = offset)
            ApiResult.Success(users)
        } catch (e: HttpException) {
            ApiResult.Error(
                code = e.code(),
                message = extractHttpErrorMessage(e) ?: "KhÃ´ng thá»ƒ táº£i danh sÃ¡ch user (${e.code()})",
                throwable = e
            )
        } catch (e: IOException) {
            ApiResult.Error(message = "Lá»—i máº¡ng", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    private fun extractHttpErrorMessage(e: HttpException): String? {
        val raw = e.response()?.errorBody()?.string()?.trim().orEmpty()
        if (raw.isBlank()) return null

        return try {
            val json = JsonParser.parseString(raw)
            if (!json.isJsonObject) {
                return raw
            }

            val messageElement = json.asJsonObject.get("message") ?: return raw
            when {
                messageElement.isJsonPrimitive -> messageElement.asString
                messageElement.isJsonArray -> messageElement.asJsonArray.firstOrNull()?.asString
                else -> raw
            }
        } catch (_: Exception) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val errorMap: Map<String, String> = gson.fromJson(raw, type)
                errorMap["message"] ?: raw
            } catch (_: Exception) {
                raw
            }
        }
    }

    private fun normalizeForgotOtpErrorMessage(code: Int, backendMessage: String?): String {
        val normalized = backendMessage?.lowercase().orEmpty()
        val isInvalidOtp =
            code == 400 ||
                code == 401 ||
                normalized.contains("otp") ||
                normalized.contains("invalid") ||
                normalized.contains("expired") ||
                normalized.contains("không hợp lệ") ||
                normalized.contains("hết hạn")

        return if (isInvalidOtp) {
            "Mã OTP không hợp lệ hoặc đã hết hạn"
        } else {
            backendMessage?.takeIf { it.isNotBlank() } ?: "Xác thực OTP thất bại ($code)"
        }
    }
}


