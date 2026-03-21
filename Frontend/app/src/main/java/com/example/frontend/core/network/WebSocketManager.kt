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

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers.asStateFlow()

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
                    // Thử bắt buộc dùng websocket nếu polling bị 404
                    options.transports = arrayOf(WebSocket.NAME)

                    if (!token.isNullOrEmpty()) {
                        options.auth = mapOf("token" to token)
                    }

                    // Xử lý URL chuẩn: Xóa dấu / ở cuối và bỏ /api
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
    }

    fun disconnect() {
        Log.d("WebSocket", "Đang ngắt kết nối Socket...")
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
        _isConnected.value = false
    }

    fun sendMessage(conversationId: String, content: String, type: String) {
        if (mSocket?.connected() == true) {
            try {
                val payload = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("content", content)
                    put("type", type)
                }
                mSocket?.emit("send_message", payload)
            } catch (e: Exception) {
                Log.e("WebSocket", "Lỗi khi gửi tin nhắn: ${e.message}")
            }
        } else {
            Log.e("WebSocket", "Không thể gửi tin nhắn do chưa kết nối Socket")
        }
    }
}
