package com.example.frontend.data.remote.dto

data class ConversationParticipantDto(
    val id: String?,
    val displayName: String?,
    val username: String?,
    val avatarUrl: String?
)

data class ConversationDto(
    val id: String?,
    val participants: List<ConversationParticipantDto>?
)

data class CreateConversationRequest(
    val participantIds: List<String>
)

data class CreateConversationResponse(
    val id: String,
    val reused: Boolean
)