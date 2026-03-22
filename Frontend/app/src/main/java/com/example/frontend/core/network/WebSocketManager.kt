package com.example.frontend.core.network

import android.util.Log
import com.example.frontend.core.config.AppConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class TypingInfo(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean
)

@Singleton
class WebSocketManager @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    private var mSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _incomingMessages = MutableStateFlow<String?>(null)
    val incomingMessages: StateFlow<String?> = _incomingMessages

    private val _messageSentSuccess = MutableStateFlow<String?>(null)
    val messageSentSuccess: StateFlow<String?> = _messageSentSuccess.asStateFlow()

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers.asStateFlow()

    private val _typingEvents = MutableStateFlow<TypingInfo?>(null)
    val typingEvents: StateFlow<TypingInfo?> = _typingEvents.asStateFlow()

    fun connect() {
        scope.launch {
            if (mSocket?.connected() == true) {
                Log.d("WebSocket", "Socket đã được kết nối rồi.")
                return@launch
            }

            try {
                val token = tokenProvider.getAccessToken()

                if (mSocket == null) {
                    val options = IO.Options()
                    options.reconnection = true
                    options.reconnectionDelay = 5000
                    options.transports = arrayOf(WebSocket.NAME)

                    if (!token.isNullOrEmpty()) {
                        options.auth = mapOf("token" to token)
                    }

                    val baseUrl = AppConfig.BASE_URL.trim()
                    var cleanedUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
                    
                    if (cleanedUrl.endsWith("/api")) {
                        cleanedUrl = cleanedUrl.substringBeforeLast("/api")
                    }
                    
                    Log.d("WebSocket", "BASE_URL gốc: '$baseUrl'")
                    Log.d("WebSocket", "URL kết nối sau khi xử lý: '$cleanedUrl'")
                    
                    mSocket = IO.socket(cleanedUrl, options)
                    setupListeners()
                }

                Log.d("WebSocket", "Đang gọi mSocket?.connect()...")
                mSocket?.connect()
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi khởi tạo Socket: ${e.message}", e)
            }
        }
    }

    private fun setupListeners() {
        mSocket?.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            Log.d("WebSocket", "Đã kết nối thành công tới Socket.io")
        }

        mSocket?.on(Socket.EVENT_DISCONNECT) { args ->
            _isConnected.value = false
            Log.d("WebSocket", "Đã đóng kết nối: ${args.getOrNull(0)}")
        }

        mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.getOrNull(0)
            _isConnected.value = false
            Log.e("WebSocket", "Lỗi kết nối Socket (EVENT_CONNECT_ERROR): $error")
            if (error is Throwable) {
                error.printStackTrace()
            }
        }

        mSocket?.on("new_message") { args ->
            val data = args.getOrNull(0)?.toString()
            _incomingMessages.value = data
            Log.d("WebSocket", "Có tin nhắn mới: $data")
        }

        mSocket?.on("message_sent_success") { args ->
            val data = args.getOrNull(0)?.toString()
            _messageSentSuccess.value = data
            Log.d("WebSocket", "Gửi tin nhắn thành công: $data")
        }

        mSocket?.on("online_users_list") { args ->
            try {
                val dataStr = args.getOrNull(0)?.toString()
                if (dataStr != null) {
                    val jsonArray = JSONArray(dataStr)
                    val users = mutableSetOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        users.add(jsonArray.getString(i))
                    }
                    _onlineUsers.value = users
                    Log.d("WebSocket", "Online users: $users")
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi parse online_users_list: ${e.message}")
            }
        }

        mSocket?.on("user_online") { args ->
            try {
                val dataStr = args.getOrNull(0)?.toString()
                if (dataStr != null) {
                    val jsonObj = JSONObject(dataStr)
                    val userId = jsonObj.optString("userId")
                    if (userId.isNotEmpty()) {
                        _onlineUsers.value = _onlineUsers.value + userId
                        Log.d("WebSocket", "User online: $userId")
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi parse user_online: ${e.message}")
            }
        }

        mSocket?.on("user_offline") { args ->
            try {
                val dataStr = args.getOrNull(0)?.toString()
                if (dataStr != null) {
                    val jsonObj = JSONObject(dataStr)
                    val userId = jsonObj.optString("userId")
                    if (userId.isNotEmpty()) {
                        _onlineUsers.value = _onlineUsers.value - userId
                        Log.d("WebSocket", "User offline: $userId")
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi parse user_offline: ${e.message}")
            }
        }

        mSocket?.on("is_typing") { args ->
            try {
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    val conversationId = data.getString("conversationId")
                    val userId = data.getString("userId")
                    val isTyping = data.getBoolean("isTyping")
                    Log.d("WebSocket", "<<< Nhận event 'is_typing': from=$userId, isTyping=$isTyping")
                    _typingEvents.value = TypingInfo(conversationId, userId, isTyping)
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi parse is_typing: ${e.message}")
            }
        }
    }

    fun disconnect() {
        Log.d("WebSocket", "Đang ngắt kết nối Socket...")
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
        _isConnected.value = false
    }

    fun sendMessage(
        conversationId: String,
        content: String?,
        type: String,
        mediaId: String? = null,
        temporaryId: String? = null,
        replyToId: String? = null
    ) {
        if (mSocket?.connected() == true) {
            try {
                val payload = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("content", content)
                    put("type", type)
                    mediaId?.let { put("mediaId", it) }
                    temporaryId?.let { put("temporaryId", it) }
                    replyToId?.let { put("replyToId", it) }
                }
                mSocket?.emit("send_message", payload)
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi khi gửi tin nhắn: ${e.message}")
            }
        } else {
            Log.e("WebSocket", "Không thể gửi tin nhắn do chưa kết nối Socket")
        }
    }

    fun sendTypingEvent(conversationId: String, isTyping: Boolean) {
        if (mSocket?.connected() == true) {
            try {
                val payload = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("isTyping", isTyping)
                }
                Log.d("WebSocket", ">>> Gửi event 'typing': convId=$conversationId, isTyping=$isTyping")
                mSocket?.emit("typing", payload)
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi khi gửi typing event: ${e.message}")
            }
        } else {
            Log.e("WebSocket", "Socket chưa kết nối, không thể gửi event typing")
        }
    }
}
