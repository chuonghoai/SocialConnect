package com.example.frontend.core.network

interface TokenProvider {
    suspend fun getAccessToken(): String?

    /** Đọc token từ bộ nhớ (không suspend – dùng trong OkHttp Interceptor). */
    fun getCachedToken(): String?

    /** Xóa access token khi phiên đăng nhập không còn hợp lệ (401). */
    suspend fun clearAccessToken()
}
