package com.example.frontend.data.remote.dto

data class SignatureBaseResponse(val success: Boolean, val data: SignatureData)
data class SignatureData(
    val signature: String,
    val timestamp: Long,
    val folder: String,
    val api_key: String,
    val cloud_name: String,
    val tags: String
)

data class CloudinaryResponseDto(
    val secure_url: String,
    val public_id: String,
    val resource_type: String,
    val format: String,
    val width: Int? = null,
    val height: Int? = null,
    val bytes: Long,
    val duration: Double? = null,
    val is_audio: Boolean? = false
)

data class SaveMediaRequestDto(
    val url: String,
    val public_id: String,
    val resource_type: String,
    val format: String,
    val width: Int?,
    val height: Int?,
    val bytes: Long,
    val duration: Double?,
    val is_audio: Boolean
)
data class SaveMediaBaseResponse(val success: Boolean, val data: SaveMediaData)
data class SaveMediaData(val id: String)