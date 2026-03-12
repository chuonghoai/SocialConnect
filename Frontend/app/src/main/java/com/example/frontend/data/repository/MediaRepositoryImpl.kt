package com.example.frontend.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.MediaApi
import com.example.frontend.data.remote.dto.SaveMediaRequestDto
import com.example.frontend.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val mediaApi: MediaApi,
    @ApplicationContext private val context: Context
) : MediaRepository {

    override suspend fun uploadMedia(uri: Uri): ApiResult<String> {
        return try {
            val sigRes = mediaApi.getSignature().data

            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return ApiResult.Error(message = "Không thể đọc file từ thiết bị")

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val fileName = "upload.$extension"

            val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
            fun createPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())

            val cloudinaryUrl = "https://api.cloudinary.com/v1_1/${sigRes.cloud_name}/auto/upload"

            val cloudRes = mediaApi.uploadToCloudinary(
                url = cloudinaryUrl,
                file = filePart,
                apiKey = createPart(sigRes.api_key),
                timestamp = createPart(sigRes.timestamp.toString()),
                signature = createPart(sigRes.signature),
                folder = createPart(sigRes.folder),
                tags = createPart(sigRes.tags)
            )

            val saveReq = SaveMediaRequestDto(
                url = cloudRes.secure_url,
                public_id = cloudRes.public_id,
                resource_type = cloudRes.resource_type,
                format = cloudRes.format,
                width = cloudRes.width,
                height = cloudRes.height,
                bytes = cloudRes.bytes,
                duration = cloudRes.duration ?: 0.0,
                is_audio = cloudRes.is_audio ?: false
            )
            val saveRes = mediaApi.saveMedia(saveReq)

            ApiResult.Success(saveRes.data.id)

        } catch (e: Exception) {
            ApiResult.Error(message = "Lỗi Upload: ${e.message}", throwable = e)
        }
    }
}