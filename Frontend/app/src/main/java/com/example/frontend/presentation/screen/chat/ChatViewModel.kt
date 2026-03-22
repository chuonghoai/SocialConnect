package com.example.frontend.presentation.screen.chat

import MessageItem
import NewMessageEvent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.ConversationUseCase.GetMessagesUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val currentUser: User? = null,
    val onlineUsers: Set<String> = emptySet(),
    val isPartnerTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val webSocketManager: WebSocketManager,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentConversationId: String? = null
    private var typingJob: Job? = null
    private var isCurrentlyTyping = false

    init {
        fetchCurrentUser()
        observeSocketStatus()
        observeIncomingMessages()
        observeOnlineUsers()
        observeTypingEvents()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            when (val result = getMeUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(currentUser = result.data)
                }
                else -> {
                    Log.e("ChatViewModel", "Không thể lấy thông tin người dùng hiện tại")
                }
            }
        }
    }

    private fun observeSocketStatus() {
        viewModelScope.launch {
            webSocketManager.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    private fun observeOnlineUsers() {
        viewModelScope.launch {
            webSocketManager.onlineUsers.collect { users ->
                _uiState.value = _uiState.value.copy(onlineUsers = users)
            }
        }
    }

    private fun observeTypingEvents() {
        viewModelScope.launch {
            webSocketManager.typingEvents.collect { typingInfo ->
                if (typingInfo != null && typingInfo.conversationId == currentConversationId && typingInfo.userId != _uiState.value.currentUser?.id) {
                    _uiState.value = _uiState.value.copy(isPartnerTyping = typingInfo.isTyping)
                }
            }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketManager.incomingMessages.collect { json ->
                if (json != null) {
                    try {
                        val event = gson.fromJson(json, NewMessageEvent::class.java)
                        val newMessage = event.message
                        
                        if (currentConversationId == null || event.conversationId == currentConversationId) {
                            _uiState.value = _uiState.value.copy(
                                messages = (listOf(newMessage) + _uiState.value.messages)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi parse tin nhắn mới: ${e.message}")
                    }
                }
            }
        }
    }

    fun loadMessages(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getMessagesUseCase(conversationId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = result.data?.messages ?: emptyList()
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun sendChatMessage(conversationId: String, content: String, type: String = "TEXT") {
        if (content.isBlank()) return
        webSocketManager.sendMessage(conversationId, content, type)
        // Reset typing status when message is sent
        sendTypingStatus(false)
    }

    fun onTyping(text: String) {
        if (currentConversationId == null) return

        if (!isCurrentlyTyping && text.isNotEmpty()) {
            isCurrentlyTyping = true
            sendTypingStatus(true)
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(3000)
            isCurrentlyTyping = false
            sendTypingStatus(false)
        }
    }

    private fun sendTypingStatus(isTyping: Boolean) {
        currentConversationId?.let {
            webSocketManager.sendTypingEvent(it, isTyping)
        }
    }
}
