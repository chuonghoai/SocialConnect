package com.example.frontend.core.network

import okhttp3.Interceptor
import okhttp3.Response

class JwtInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Đọc từ in-memory cache – không block OkHttp thread,
        // không gây ANR.
        // Cache được populate bởi StartViewModel.init (suspend getAccessToken())
        // trước khi bất kỳ API call nào xảy ra.
        val token = tokenProvider.getCachedToken()

        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
