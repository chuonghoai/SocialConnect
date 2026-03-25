package com.example.frontend.presentation.screen.calls

import org.webrtc.VideoTrack

data class CallUiState(
    val status: String = "idle", // "connecting", "ringing", "accepted", "rejected", "ended"
    val isCalling: Boolean = false,
    val targetUserId: String = "",
    val callerName: String = "",
    val callerAvatarUrl: String = "",
    val isVideoCall: Boolean = false,
    val isIncomingCall: Boolean = false,
    val formattedDuration: String = "00:00",
    val isMicOn: Boolean = true,
    val isCamOn: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val errorMessage: String? = null,
    val localVideoTrack: VideoTrack? = null,
    val remoteVideoTrack: VideoTrack? = null
)

sealed class CallUiEvent {
    data class IncomingCall(val callerId: String, val name: String, val avatar: String, val isVideo: Boolean) : CallUiEvent()
    data class CallAccepted(val responderId: String) : CallUiEvent()
    data class CallDeclined(val responderId: String) : CallUiEvent()
    data class CallEnded(val userId: String) : CallUiEvent()
    data class CallError(val message: String) : CallUiEvent()
    data class WebrtcOfferReceived(val senderId: String, val sdp: org.json.JSONObject) : CallUiEvent()
    data class WebrtcAnswerReceived(val senderId: String, val sdp: org.json.JSONObject) : CallUiEvent()
    data class WebrtcIceCandidateReceived(val senderId: String, val candidate: org.json.JSONObject) : CallUiEvent()
}
