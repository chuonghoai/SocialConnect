package com.example.frontend.data.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.datastore.TokenDataStore
import com.example.frontend.data.mapper.toDomain
import com.example.frontend.data.remote.api.AuthApi
import com.example.frontend.data.remote.dto.ApiErrorDto
import com.example.frontend.data.remote.dto.LoginRequestDto
import com.example.frontend.data.remote.dto.RegisterRequestDto
import com.example.frontend.data.remote.dto.sendOtpRequestDto
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java
import com.google.gson.Gson

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    private val gson = Gson()

    override suspend fun getMe(): ApiResult<User> {
        return try {
            val dto = authApi.me()
            ApiResult.Success(dto.toDomain())
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = e.message(), throwable = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Network error", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun login(username: String, password: String): ApiResult<Unit> {
        return try {
            val res = authApi.login(LoginRequestDto(username, password))
            tokenDataStore.saveAccessToken(res.accessToken)
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string()
            val msg = try {
                raw?.let { gson.fromJson(it, ApiErrorDto::class.java)?.message }
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
            ApiResult.Error(message = "Network error", throwable = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "Unexpected error: ${e.message}", throwable = e)
        }
    }

    override suspend fun logout() {
        tokenDataStore.clear()
    }

    override suspend fun register(email: String, password: String, mailOtp: String): ApiResult<Unit> {
        return try {
            val res = authApi.register(RegisterRequestDto(email, password, mailOtp))
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
            val res = authApi.sendMailOtp(sendOtpRequestDto(email, type))
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
