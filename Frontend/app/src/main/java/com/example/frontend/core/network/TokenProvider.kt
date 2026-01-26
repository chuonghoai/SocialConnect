package com.example.frontend.core.network

interface TokenProvider {
    suspend fun getAccessToken(): String?
}
