package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.User

interface AuthRepository {
    suspend fun getMe(isRefresh: Boolean = false): ApiResult<User>
    suspend fun login(username: String, password: String): ApiResult<Unit>
    suspend fun logout()
    suspend fun register(email: String, password: String, mailOtp: String): ApiResult<Unit>

    suspend fun sendOtp(email: String, type: String): ApiResult<Unit>
}
