package com.example.frontend.domain.model

data class Conversation(
    val id: String,
    val createAt: String,
    val updateAt: String,
    val unreadCount: Int,
    val participants: List<Participant>,
    val lastMessage: LastMessage?
)

data class Participant(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val isOnline: Boolean
)

data class LastMessage(
    val id: String,
    val type: String,
    val text: String,
    val isRecall: Boolean,
    val createAt: String,
    val sender: MessageSender
)

data class MessageSender(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)
