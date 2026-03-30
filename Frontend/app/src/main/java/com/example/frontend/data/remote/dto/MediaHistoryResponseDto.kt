package com.example.frontend.data.remote.dto

import com.example.frontend.domain.model.MediaHistoryItem

data class MediaHistoryItemDto(
    val publicId: String,
    val secureUrl: String,
    val type: String,
    val messageId: String,
    val senderId: String,
    val createAt: String
) {
    fun toDomain() = MediaHistoryItem(
        publicId = publicId,
        secureUrl = secureUrl,
        type = type,
        messageId = messageId,
        senderId = senderId,
        createAt = createAt
    )
}

data class MediaHistoryResponseDto(
    val medias: List<MediaHistoryItemDto>
)