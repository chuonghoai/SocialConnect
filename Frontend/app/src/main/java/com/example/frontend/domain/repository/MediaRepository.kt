package com.example.frontend.domain.repository
import android.net.Uri
import com.example.frontend.core.network.ApiResult

interface MediaRepository {
    suspend fun uploadMedia(uri: Uri): ApiResult<String>
}