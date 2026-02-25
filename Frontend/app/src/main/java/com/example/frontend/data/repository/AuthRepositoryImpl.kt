package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.datastore.TokenDataStore
import com.example.frontend.data.local.dao.UserDao
import com.example.frontend.data.local.entity.toEntity
import com.example.frontend.data.remote.api.AuthApi
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore,
    private val userDao: UserDao
) : AuthRepository {

    private val gson = Gson()

    override suspend fun getMe(): ApiResult<User> {
        return try {
            val user = authApi.me()
            userDao.clearUser()
            userDao.insertUser(user.toEntity())
            ApiResult.Success(user)
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = e.message(), throwable = e)
        } catch (e: IOException) {
            val localUser = userDao.getUser()
            if (localUser != null) {
                ApiResult.Success(localUser.toDomain())
            } else {
                ApiResult.Error(message = "Không thể tải thông tin người dùng", throwable = e)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
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
        tokenDataStore.clear()
        userDao.clearUser()
    }

    override suspend fun register(
        email: String,
        password: String,
        mailOtp: String
    ): ApiResult<Unit> {
        return try {
            val body = mapOf("email" to email, "password" to password, "mailOtp" to mailOtp)
            val res = authApi.register(body)
            tokenDataStore.saveAccessToken(res.accessToken)
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
}
