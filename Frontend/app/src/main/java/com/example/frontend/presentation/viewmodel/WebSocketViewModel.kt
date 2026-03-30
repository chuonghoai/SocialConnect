package com.example.frontend.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.frontend.core.network.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebSocketViewModel @Inject constructor(
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    val isConnected = webSocketManager.isConnected
    val incomingMessages = webSocketManager.incomingMessages
    val onlineUsers = webSocketManager.onlineUsers

    fun connect() {
        webSocketManager.connect()
    }

    fun disconnect() {
        webSocketManager.disconnect()
    }

    fun sendMessage(conversationId: String, content: String, type: String) {
        webSocketManager.sendMessage(conversationId, content, type)
    }
}