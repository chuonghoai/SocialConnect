package com.example.frontend.presentation.screen.chat

import MessageItem
import MessageSender
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
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val currentUser: User? = null,
    val onlineUsers: Set<String> = emptySet(),
    val isPartnerTyping: Boolean = false,
    val isPartnerReadLatest: Boolean = false // Flag để biết đối phương đã đọc tin nhắn mới nhất chưa
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
        observeMessageSentSuccess()
        observeMessagesRead()
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

                        if (event.conversationId == currentConversationId) {
                            webSocketManager.markRead(event.conversationId)
                            
                            // Nếu mình nhận được tin nhắn từ người khác, thì flag "đối phương đã đọc tin của mình" không còn quan trọng cho tin cũ nữa
                            // Nhưng thường flag này dùng cho tin nhắn CUỐI CÙNG LÀ CỦA MÌNH.
                        }
                        
                        if (currentConversationId == null || event.conversationId == currentConversationId) {
                            val exists = _uiState.value.messages.any { it.id == newMessage.id }
                            if (!exists) {
                                _uiState.value = _uiState.value.copy(
                                    messages = (listOf(newMessage) + _uiState.value.messages)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi parse tin nhắn mới: ${e.message}")
                    }
                }
            }
        }
    }

    private fun observeMessageSentSuccess() {
        viewModelScope.launch {
            webSocketManager.messageSentSuccess.collect { json ->
                if (json != null) {
                    try {
                        val jsonObj = JSONObject(json)
                        val tempId = jsonObj.optString("temporaryId")
                        val actualMessageJson = jsonObj.optJSONObject("message")?.toString()
                        
                        if (tempId.isNotEmpty() && actualMessageJson != null) {
                            val actualMessage = gson.fromJson(actualMessageJson, MessageItem::class.java)
                            
                            val updatedMessages = _uiState.value.messages.map {
                                if (it.id == tempId) actualMessage else it
                            }
                            _uiState.value = _uiState.value.copy(messages = updatedMessages)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi xử lý message_sent_success: ${e.message}")
                    }
                }
            }
        }
    }

    private fun observeMessagesRead() {
        viewModelScope.launch {
            webSocketManager.messagesReadEvent.collect { json ->
                Log.d("ChatDebug", "ViewModel nhận tin nhắn đã đọc: $json")
                if (json != null) {
                    try {
                        val jsonObj = JSONObject(json)
                        val convId = jsonObj.optString("conversationId")
                        val readerId = jsonObj.optString("userId")

                        if (convId == currentConversationId && readerId != _uiState.value.currentUser?.id) {
                            Log.d("ChatDebug", "Khớp điều kiện! Cập nhật isPartnerReadLatest = true")
                            _uiState.value = _uiState.value.copy(isPartnerReadLatest = true)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi parse messagesReadEvent: ${e.message}")
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
                        messages = result.data?.messages ?: emptyList(),
                        isPartnerReadLatest = false // Reset khi load mới, BE sẽ emit event nếu họ đã đọc
                    )
                    webSocketManager.markRead(conversationId)
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
        
        val currentUser = _uiState.value.currentUser ?: return
        val tempId = "temp_${UUID.randomUUID()}"
        
        val tempMessage = MessageItem(
            id = tempId,
            type = type,
            text = content,
            isRecall = false,
            createAt = java.time.ZonedDateTime.now().toString(),
            replyToMessageId = null,
            sender = MessageSender(
                id = currentUser.id,
                displayName = currentUser.displayName ?: currentUser.username,
                avatarUrl = currentUser.avatarUrl
            ),
            media = emptyList()
        )
        
        // Khi mình gửi tin mới, mặc định đối phương chưa đọc tin này
        _uiState.value = _uiState.value.copy(
            messages = listOf(tempMessage) + _uiState.value.messages,
            isPartnerReadLatest = false
        )

        viewModelScope.launch {
            val success = withTimeoutOrNull(10000) {
                webSocketManager.sendMessage(
                    conversationId = conversationId,
                    content = content,
                    type = type,
                    temporaryId = tempId
                )
                true
            }
            
            if (success == null) {
                val updatedMessages = _uiState.value.messages.map {
                    if (it.id == tempId) it.copy(id = "failed_${tempId}") else it
                }
                _uiState.value = _uiState.value.copy(messages = updatedMessages)
            }
        }
        
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
