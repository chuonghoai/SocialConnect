package com.example.frontend.core.network

interface TokenProvider {
    suspend fun getAccessToken(): String?

    /** Đọc token từ bộ nhớ (không suspend – dùng trong OkHttp Interceptor). */
    fun getCachedToken(): String?
}
