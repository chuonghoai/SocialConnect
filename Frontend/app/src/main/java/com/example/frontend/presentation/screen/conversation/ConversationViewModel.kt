package com.example.frontend.presentation.screen.conversation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.usecase.ConversationUseCase.GetConversationsUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)


@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val webSocketManager: WebSocketManager,
    private val gson: Gson

) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        observeIncomingMessages()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getConversationsUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = result.data ?: emptyList()
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

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketManager.incomingMessages.collect { json ->
                if (json != null) {
                    try {
                        val event = gson.fromJson(json, NewMessageEvent::class.java)
                        val updatedConversation = event.conversation

                        // Đẩy conversation vừa có tin nhắn lên đầu
                        val currentList = _uiState.value.conversations.toMutableList()
                        currentList.removeAll { it.id == updatedConversation.id }
                        currentList.add(0, updatedConversation)

                        _uiState.value = _uiState.value.copy(conversations = currentList)
                    } catch (e: Exception) {
                        Log.e("ConversationViewModel", "Lỗi update danh sách chat: ${e.message}")
                    }
                }
            }
        }
    }
}
