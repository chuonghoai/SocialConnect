package com.example.frontend.core.network

import android.util.Log
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingCallEvent(
    val callerId: String,
    val displayName: String,
    val avatarUrl: String,
    val isVideoCall: Boolean
)

data class CallResponseEvent(
    val responderId: String,
    val accepted: Boolean
)

data class WebRTCEvent(
    val senderId: String,
    val data: JSONObject // sdp or candidate
)

@Singleton
class CallsSocketHandler @Inject constructor(
    private val webSocketManager: WebSocketManager
) {
    private val _incomingCall = MutableSharedFlow<IncomingCallEvent>(extraBufferCapacity = 1)
    val incomingCall: SharedFlow<IncomingCallEvent> = _incomingCall.asSharedFlow()

    private val _callResponse = MutableSharedFlow<CallResponseEvent>(extraBufferCapacity = 1)
    val callResponse: SharedFlow<CallResponseEvent> = _callResponse.asSharedFlow()

    private val _webrtcOffer = MutableSharedFlow<WebRTCEvent>(extraBufferCapacity = 1)
    val webrtcOffer: SharedFlow<WebRTCEvent> = _webrtcOffer.asSharedFlow()

    private val _webrtcAnswer = MutableSharedFlow<WebRTCEvent>(extraBufferCapacity = 1)
    val webrtcAnswer: SharedFlow<WebRTCEvent> = _webrtcAnswer.asSharedFlow()

    private val _webrtcIceCandidate = MutableSharedFlow<WebRTCEvent>(extraBufferCapacity = 1)
    val webrtcIceCandidate: SharedFlow<WebRTCEvent> = _webrtcIceCandidate.asSharedFlow()

    private val _callEnded = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callEnded: SharedFlow<String> = _callEnded.asSharedFlow()

    private val _callError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callError: SharedFlow<String> = _callError.asSharedFlow()

    fun initListeners(socket: Socket) {
        socket.on("incoming_call") { args ->
            val data = args[0] as JSONObject
            _incomingCall.tryEmit(
                IncomingCallEvent(
                    callerId = data.getString("callerId"),
                    displayName = data.getString("displayName"),
                    avatarUrl = data.getString("avatarUrl"),
                    isVideoCall = data.getBoolean("isVideoCall")
                )
            )
        }

        socket.on("call_response_received") { args ->
            val data = args[0] as JSONObject
            _callResponse.tryEmit(
                CallResponseEvent(
                    responderId = data.getString("responderId"),
                    accepted = data.getBoolean("accepted")
                )
            )
        }

        socket.on("webrtc_offer_received") { args ->
            val data = args[0] as JSONObject
            _webrtcOffer.tryEmit(
                WebRTCEvent(
                    senderId = data.getString("senderId"),
                    data = data.getJSONObject("sdp")
                )
            )
        }

        socket.on("webrtc_answer_received") { args ->
            val data = args[0] as JSONObject
            _webrtcAnswer.tryEmit(
                WebRTCEvent(
                    senderId = data.getString("senderId"),
                    data = data.getJSONObject("sdp")
                )
            )
        }

        socket.on("webrtc_ice_candidate_received") { args ->
            val data = args[0] as JSONObject
            _webrtcIceCandidate.tryEmit(
                WebRTCEvent(
                    senderId = data.getString("senderId"),
                    data = data.getJSONObject("candidate")
                )
            )
        }

        socket.on("call_ended") { args ->
            val data = args[0] as JSONObject
            _callEnded.tryEmit(data.getString("senderId"))
        }

        socket.on("call_error") { args ->
            val data = args[0] as JSONObject
            _callError.tryEmit(data.getString("message"))
        }
    }

    fun makeCall(targetUserId: String, isVideoCall: Boolean) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("isVideoCall", isVideoCall)
        }
        webSocketManager.emit("call_user", payload)
    }

    fun respondToCall(targetUserId: String, accepted: Boolean) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("accepted", accepted)
        }
        webSocketManager.emit("call_response", payload)
    }

    fun sendOffer(targetUserId: String, sdp: JSONObject) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("sdp", sdp)
        }
        webSocketManager.emit("webrtc_offer", payload)
    }

    fun sendAnswer(targetUserId: String, sdp: JSONObject) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("sdp", sdp)
        }
        webSocketManager.emit("webrtc_answer", payload)
    }

    fun sendIceCandidate(targetUserId: String, candidate: JSONObject) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("candidate", candidate)
        }
        webSocketManager.emit("webrtc_ice_candidate", payload)
    }

    fun endCall(targetUserId: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
        }
        webSocketManager.emit("end_call", payload)
    }
}
