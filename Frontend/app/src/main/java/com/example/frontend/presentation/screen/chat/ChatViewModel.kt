package com.example.frontend.presentation.screen.chat

import MessageItem
import MessageMedia
import MessageSender
import NewMessageEvent
import RepliedMessageInfo
import android.app.NotificationManager
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.ConversationUseCase.GetMessageContextUseCase
import com.example.frontend.domain.usecase.ConversationUseCase.GetMessagesUseCase
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import com.example.frontend.ui.component.NotificationType
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
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
    val selectedMedia: List<Uri> = emptyList(),
    val isUploadingMedia: Boolean = false,
    
    // Voice recording states
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val recordingFileUri: Uri? = null,
    val showVoiceRecorder: Boolean = false,
    val recordingAmplitude: Float = 0f
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getMessageContextUseCase: GetMessageContextUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val webSocketManager: WebSocketManager,
    private val appNotificationManager: AppNotificationManager,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentConversationId: String? = null
    private var typingJob: Job? = null
    private var isCurrentlyTyping = false

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingJob: Job? = null
    private var isSuccessfullyStarted = false

    var highlightedMessageId by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    private val _scrollEvent = kotlinx.coroutines.flow.MutableSharedFlow<Int>()
    val scrollEvent = _scrollEvent.asSharedFlow()

    init {
        fetchCurrentUser()
        observeSocketStatus()
        observeIncomingMessages()
        observeMessageSentSuccess()
        observeMessagesRead()
        observeOnlineUsers()
        observeTypingEvents()
        observeRevokedMessages()
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

    private fun observeRevokedMessages() {
        viewModelScope.launch {
            webSocketManager.messageRevokedFlow.collect { payload ->
                val evtConversationId = payload.optString("conversationId")
                val evtMessageId = payload.optString("messageId")

                if (evtConversationId == currentConversationId) {
                    updateMessageToRecalled(evtMessageId)
                }
            }
        }
    }

    private fun observeMessagesRead() {
        viewModelScope.launch {
            webSocketManager.messagesReadEvent.collect { json ->
                if (json != null) {
                    try {
                        val jsonObj = JSONObject(json)
                        val convId = jsonObj.optString("conversationId")
                        val readerId = jsonObj.optString("userId")

                        if (convId == currentConversationId && readerId != _uiState.value.currentUser?.id) {
                            val updatedMessages = _uiState.value.messages.map { msg ->
                                if (msg.sender.id == _uiState.value.currentUser?.id) {
                                    msg.copy(isRead = true)
                                } else msg
                            }
                            _uiState.value = _uiState.value.copy(messages = updatedMessages)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi observeMessagesRead: ${e.message}")
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

    fun onMediaSelected(uri: Uri) {
        val current = _uiState.value.selectedMedia.toMutableList()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _uiState.value = _uiState.value.copy(selectedMedia = current)
    }

    fun sendChatWithMedia(
        conversationId: String,
        content: String?,
        replyToId: String? = null,
        replyToMessageInfo: RepliedMessageInfo? = null
    ) {
        val currentUser = _uiState.value.currentUser ?: return
        val selectedMedia = _uiState.value.selectedMedia.toList() // Copy list
        
        _uiState.value = _uiState.value.copy(selectedMedia = emptyList())

        viewModelScope.launch {
            if (!content.isNullOrBlank()) {
                sendChatMessage(conversationId, content, replyToId = replyToId , replyToMessageInfo = replyToMessageInfo)
            }

            if (selectedMedia.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(isUploadingMedia = true)
                selectedMedia.forEach { uri ->
                    val tempId = "temp_media_${UUID.randomUUID()}"
                    
                    val tempMessage = MessageItem(
                        id = tempId,
                        type = "MEDIA",
                        text = "",
                        isRecall = false,
                        createAt = java.time.ZonedDateTime.now().toString(),
                        replyToMessage = null,
                        sender = MessageSender(
                            id = currentUser.id,
                            displayName = currentUser.displayName ?: currentUser.username,
                            avatarUrl = currentUser.avatarUrl
                        ),
                        media = listOf(MessageMedia(publicId = "", secureUrl = uri.toString(), type = "IMAGE")),
                        isRead = false
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        messages = (listOf(tempMessage) + _uiState.value.messages)
                    )

                    when (val uploadResult = uploadMediaUseCase(uri)) {
                        is ApiResult.Success -> {
                            val mediaId = uploadResult.data
                            if (mediaId != null) {
                                webSocketManager.sendMessage(
                                    conversationId = conversationId,
                                    content = null,
                                    type = "MEDIA",
                                    mediaId = mediaId,
                                    temporaryId = tempId
                                )
                            }
                        }
                        is ApiResult.Error -> {
                            val updated = _uiState.value.messages.map {
                                if (it.id == tempId) it.copy(id = "failed_${tempId}") else it
                            }
                            _uiState.value = _uiState.value.copy(messages = updated)
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isUploadingMedia = false)
            }
        }
    }

    fun sendChatMessage(
        conversationId: String,
        content: String,
        type: String = "TEXT",
        replyToId: String? = null,
        replyToMessageInfo: RepliedMessageInfo? = null
    ) {
        if (content.isBlank()) return
        
        val currentUser = _uiState.value.currentUser ?: return
        val tempId = "temp_${UUID.randomUUID()}"
        
        val tempMessage = MessageItem(
            id = tempId,
            type = type,
            text = content,
            isRecall = false,
            createAt = java.time.ZonedDateTime.now().toString(),
            replyToMessage = replyToMessageInfo,
            sender = MessageSender(
                id = currentUser.id,
                displayName = currentUser.displayName ?: currentUser.username,
                avatarUrl = currentUser.avatarUrl
            ),
            media = emptyList(),
            isRead = false
        )
        
        _uiState.value = _uiState.value.copy(
            messages = (listOf(tempMessage) + _uiState.value.messages)
        )

        viewModelScope.launch {
            val success = withTimeoutOrNull(10000) {
                webSocketManager.sendMessage(
                    conversationId = conversationId,
                    content = content,
                    type = type,
                    temporaryId = tempId,
                    replyToId = replyToId
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

    // Voice Recording Logic
    fun toggleVoiceRecorder(show: Boolean) {
        _uiState.value = _uiState.value.copy(showVoiceRecorder = show)
        if (!show) {
            cancelRecording()
        }
    }

    fun startRecording() {
        try {
            isSuccessfullyStarted = false
            recordingFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            isSuccessfullyStarted = true
            _uiState.value = _uiState.value.copy(isRecording = true, recordingDuration = 0, recordingFileUri = null)
            
            recordingJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (_uiState.value.isRecording) {
                    _uiState.value = _uiState.value.copy(
                        recordingDuration = System.currentTimeMillis() - startTime,
                        recordingAmplitude = try { mediaRecorder?.maxAmplitude?.toFloat() ?: 0f } catch (e: Exception) { 0f }
                    )
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error starting recording: ${e.message}")
            isSuccessfullyStarted = false
            _uiState.value = _uiState.value.copy(isRecording = false)
        }
    }

    fun stopRecording() {
        if (!isSuccessfullyStarted) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordingJob?.cancel()
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                recordingFileUri = Uri.fromFile(recordingFile)
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error stopping recording: ${e.message}")
            cancelRecording()
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
        }
        mediaRecorder = null
        recordingJob?.cancel()
        recordingFile?.delete()
        isSuccessfullyStarted = false
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingDuration = 0,
            recordingFileUri = null,
            recordingAmplitude = 0f
        )
    }

    fun sendVoiceMessage(conversationId: String) {
        val uri = _uiState.value.recordingFileUri ?: return
        val currentUser = _uiState.value.currentUser ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingMedia = true, showVoiceRecorder = false)
            val tempId = "temp_voice_${UUID.randomUUID()}"
            
            val tempMessage = MessageItem(
                id = tempId,
                type = "AUDIO",
                text = "",
                isRecall = false,
                createAt = java.time.ZonedDateTime.now().toString(),
                replyToMessage = null,
                sender = MessageSender(
                    id = currentUser.id,
                    displayName = currentUser.displayName ?: currentUser.username,
                    avatarUrl = currentUser.avatarUrl
                ),
                media = listOf(MessageMedia(publicId = "", secureUrl = uri.toString(), type = "AUDIO")),
                isRead = false
            )
            
            _uiState.value = _uiState.value.copy(messages = (listOf(tempMessage) + _uiState.value.messages))

            when (val uploadResult = uploadMediaUseCase(uri)) {
                is ApiResult.Success -> {
                    val mediaId = uploadResult.data
                    if (mediaId != null) {
                        webSocketManager.sendMessage(
                            conversationId = conversationId,
                            content = null,
                            type = "MEDIA",
                            mediaId = mediaId,
                            temporaryId = tempId
                        )
                    }
                }
                is ApiResult.Error -> {
                    val updated = _uiState.value.messages.map {
                        if (it.id == tempId) it.copy(id = "failed_${tempId}") else it
                    }
                    _uiState.value = _uiState.value.copy(messages = updated)
                }
            }
            _uiState.value = _uiState.value.copy(isUploadingMedia = false, recordingFileUri = null)
        }
    }

    fun revokeMessage(messageId: String) {
        webSocketManager.revokeMessage(messageId, currentConversationId)

        updateMessageToRecalled(messageId)
    }

    private fun updateMessageToRecalled(messageId: String) {
        val currentMessages = _uiState.value.messages
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(isRecall = true, text = "", media = emptyList())
            } else {
                msg
            }
        }
        _uiState.value = _uiState.value.copy(messages = updatedMessages)
    }

    fun notifyMessageCopied() {
        appNotificationManager.showMessage("Đã sao chép tin nhắn", NotificationType.SUCCESS)
    }

    fun onRepliedMessageClick(messageId: String) {
        val convId = currentConversationId ?: return

        viewModelScope.launch {
            val index = uiState.value.messages.indexOfFirst { it.id == messageId }

            if (index != -1) {
                triggerHighlightAndScroll(index, messageId)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = getMessageContextUseCase(convId, messageId)) {
                    is ApiResult.Success -> {
                        val contextMessages = result.data?.messages ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            messages = contextMessages,
                            isLoading = false
                        )

                        delay(200)
                        val newIndex = _uiState.value.messages.indexOfFirst { it.id == messageId }
                        if (newIndex != -1) {
                            triggerHighlightAndScroll(newIndex, messageId)
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        Log.e("ChatViewModel", "Lỗi tải context: ${result.message}")
                    }
                }
            }
        }
    }

    private suspend fun triggerHighlightAndScroll(index: Int, messageId: String) {
        _scrollEvent.emit(index)
        highlightedMessageId = messageId
        delay(1500)
        if (highlightedMessageId == messageId) {
            highlightedMessageId = null
        }
    }
}
