package com.example.frontend.core.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: Int? = null,
        val message: String = "Unknown error",
        val throwable: Throwable? = null
    ) : ApiResult<Nothing>()
}
