package com.example.frontend.domain.repository

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.AdminUserItem
import com.example.frontend.domain.model.User

interface AuthRepository {
    suspend fun getMe(isRefresh: Boolean = false): ApiResult<User>
    suspend fun getUserProfile(userId: String): ApiResult<User>
    suspend fun login(username: String, password: String): ApiResult<Unit>
    suspend fun logout()
    suspend fun register(email: String, password: String, mailOtp: String): ApiResult<Unit>

    suspend fun sendOtp(email: String, type: String): ApiResult<Unit>
    suspend fun verifyForgotPasswordOtp(email: String, otp: String): ApiResult<Unit>
    suspend fun resetPassword(email: String, newPassword: String): ApiResult<Unit>
    suspend fun updateProfile(displayName: String, dob: String, email: String, avatar: String?): ApiResult<User>
    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit>
    suspend fun lockUser(userId: String): ApiResult<Unit>
    suspend fun unlockUser(userId: String): ApiResult<Unit>
    suspend fun deleteUser(userId: String): ApiResult<Unit>
    suspend fun getAdminUsers(limit: Int = 100, offset: Int = 0): ApiResult<List<AdminUserItem>>
}
