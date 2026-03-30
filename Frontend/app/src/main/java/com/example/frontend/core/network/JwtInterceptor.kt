package com.example.frontend.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class JwtInterceptor(
    private val tokenProvider: TokenProvider,
    private val authSessionManager: AuthSessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getCachedToken() ?: runBlocking {
            tokenProvider.getAccessToken()
        }

        val request = if (!token.isNullOrBlank()) {
            val bearerToken =
                if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

            chain.request().newBuilder()
                .addHeader("Authorization", bearerToken)
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !token.isNullOrBlank()) {
            runBlocking {
                tokenProvider.clearAccessToken()
            }
            authSessionManager.notifySessionExpired()
        }

        return response
    }
}
