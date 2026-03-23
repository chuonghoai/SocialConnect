package com.example.frontend.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.frontend.core.network.ApiResult
import com.example.frontend.data.remote.api.MediaApi
import com.example.frontend.data.remote.dto.SaveMediaRequestDto
import com.example.frontend.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
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

            // Xác định MIME Type chính xác hơn
            val mimeType = if (uri.scheme == "content") {
                context.contentResolver.getType(uri)
            } else {
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                if (extension.isNotEmpty()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                } else {
                    // Xử lý thủ công cho file từ cache có đuôi .m4a
                    if (uri.path?.endsWith(".m4a") == true) "audio/mp4" else null
                }
            } ?: "application/octet-stream"

            Log.d("Cloudinary", "Detected MimeType: $mimeType for URI: $uri")

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            val fileName = "upload.$extension"

            val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
            fun createPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())

            // Tự động xác định resource_type dựa trên mimeType
            val resourceType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType.startsWith("video/") -> "video"
                mimeType.startsWith("audio/") -> "video" // Cloudinary xử lý audio trong 'video' resource type
                mimeType.contains("mp4") || mimeType.contains("mpeg") -> "video"
                else -> "auto"
            }

            val cloudinaryUrl = "https://api.cloudinary.com/v1_1/${sigRes.cloud_name}/$resourceType/upload"
            Log.d("Cloudinary", "Uploading to: $cloudinaryUrl with resourceType: $resourceType")

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
                is_audio = cloudRes.is_audio ?: (resourceType == "video" && cloudRes.width == null) || mimeType.startsWith("audio/")
            )
            val saveRes = mediaApi.saveMedia(saveReq)

            ApiResult.Success(saveRes.data.id)

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("Cloudinary", "Lỗi Cloudinary 400: $errorBody")
            ApiResult.Error(message = "Lỗi Cloudinary 400: $errorBody", throwable = e)
        } catch (e: Exception) {
            Log.e("Cloudinary", "Lỗi Upload: ${e.message}")
            ApiResult.Error(message = "Lỗi Upload: ${e.message}", throwable = e)
        }
    }
}
