package com.example.frontend.presentation.screen.calls

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.CallsSocketHandler
import com.example.frontend.core.network.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import com.example.frontend.core.webrtc.WebRtcClient
import org.webrtc.VideoTrack

@HiltViewModel
class CallViewModel @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val callsSocketHandler: CallsSocketHandler,
    val webRtcClient: WebRtcClient,
    @ApplicationContext private val context: Context
) : ViewModel(), WebRtcClient.Listener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Khởi tạo State và Event
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CallUiEvent>()
    val uiEvent: SharedFlow<CallUiEvent> = _uiEvent.asSharedFlow()

    init {
        observeSocketConnection()
        observeCallEvents()
        setupAudio()
    }

    private fun setupAudio() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }

    private fun observeSocketConnection() {
        viewModelScope.launch {
            webSocketManager.connectedSocket.collectLatest { socket ->
                socket?.let { callsSocketHandler.initListeners(it) }
            }
        }
    }

    private fun observeCallEvents() {
        viewModelScope.launch {
            // Receiver incoming call
            callsSocketHandler.incomingCall.collect { event ->
                _uiState.value = _uiState.value.copy(
                    status = "ringing",
                    isIncomingCall = true,
                    targetUserId = event.callerId,
                    callerName = event.displayName,
                    callerAvatarUrl = event.avatarUrl,
                    isVideoCall = event.isVideoCall
                )
                _uiEvent.emit(CallUiEvent.IncomingCall(event.callerId, event.displayName, event.avatarUrl, event.isVideoCall))
            }
        }

        // Receiver response accept/reject
        viewModelScope.launch {
            callsSocketHandler.callResponse.collect { event ->
                if (event.accepted) {
                    _uiState.value = _uiState.value.copy(status = "accepted", isCalling = true)
                    webRtcClient.initializePeerConnection()
                    webRtcClient.call()
                    _uiEvent.emit(CallUiEvent.CallAccepted(event.responderId))
                } else {
                    _uiState.value = _uiState.value.copy(status = "rejected")
                    _uiEvent.emit(CallUiEvent.CallDeclined(event.responderId))
                }
            }
        }

        // Receiver offer
        viewModelScope.launch {
            callsSocketHandler.webrtcOffer.collect { event ->
                webRtcClient.initializePeerConnection()
                webRtcClient.onRemoteSessionReceived(event.data)
                webRtcClient.answer()
                _uiEvent.emit(CallUiEvent.WebrtcOfferReceived(event.senderId, event.data))
            }
        }

        // Receiver answer
        viewModelScope.launch {
            callsSocketHandler.webrtcAnswer.collect { event ->
                webRtcClient.onRemoteSessionReceived(event.data)
                _uiEvent.emit(CallUiEvent.WebrtcAnswerReceived(event.senderId, event.data))
            }
        }

        // Exchange network (ICE Candidate)
        viewModelScope.launch {
            callsSocketHandler.webrtcIceCandidate.collect { event ->
                webRtcClient.addIceCandidate(event.data)
                _uiEvent.emit(CallUiEvent.WebrtcIceCandidateReceived(event.senderId, event.data))
            }
        }

        // Receiver reject calling event
        viewModelScope.launch {
            callsSocketHandler.callEnded.collect { senderId ->
                _uiState.value = _uiState.value.copy(status = "ended")
                _uiEvent.emit(CallUiEvent.CallEnded(senderId))
                webRtcClient.endCall()
                resetAudio()
            }
        }

        // Call error
        viewModelScope.launch {
            callsSocketHandler.callError.collect { message ->
                _uiState.value = _uiState.value.copy(errorMessage = message)
                _uiEvent.emit(CallUiEvent.CallError(message))
            }
        }
    }

    fun startCall(targetUserId: String, isVideoCall: Boolean, name: String, avatar: String) {
        _uiState.value = _uiState.value.copy(
            status = "connecting",
            isCalling = true,
            targetUserId = targetUserId,
            isVideoCall = isVideoCall,
            callerName = name,
            callerAvatarUrl = avatar,
            isIncomingCall = false,
            isSpeakerOn = isVideoCall
        )
        audioManager.isSpeakerphoneOn = isVideoCall
        webRtcClient.startLocalMedia(isVideoCall)
        _uiState.value = _uiState.value.copy(localVideoTrack = webRtcClient.localVideoTrack)
        callsSocketHandler.makeCall(targetUserId, isVideoCall)
        _uiState.value = _uiState.value.copy(status = "ringing")
    }

    fun acceptCall(targetUserId: String) {
        val isVideoCall = _uiState.value.isVideoCall
        _uiState.value = _uiState.value.copy(status = "accepted")

        audioManager.isSpeakerphoneOn = isVideoCall
        webRtcClient.startLocalMedia(isVideoCall)
        _uiState.value = _uiState.value.copy(localVideoTrack = webRtcClient.localVideoTrack)

        callsSocketHandler.respondToCall(targetUserId, true)
    }

    fun declineCall(targetUserId: String) {
        _uiState.value = _uiState.value.copy(status = "rejected")
        callsSocketHandler.respondToCall(targetUserId, false)
        resetAudio()
    }

    fun endCall(targetUserId: String) {
        _uiState.value = CallUiState(status = "ended")
        callsSocketHandler.endCall(targetUserId)
        webRtcClient.endCall()
        resetAudio()
    }

    fun toggleMic() {
        val newState = !_uiState.value.isMicOn
        _uiState.value = _uiState.value.copy(isMicOn = newState)
        webRtcClient.toggleAudio(!newState)
    }

    fun toggleSpeaker() {
        val newState = !_uiState.value.isSpeakerOn
        _uiState.value = _uiState.value.copy(isSpeakerOn = newState)
        audioManager.isSpeakerphoneOn = newState
    }

    fun toggleCamera() {
        val newState = !_uiState.value.isCamOn
        _uiState.value = _uiState.value.copy(isCamOn = newState)
        webRtcClient.toggleVideo(!newState)
    }

    fun switchCamera() { webRtcClient.switchCamera() }

    private fun resetAudio() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
    }

    fun clearState() {
        _uiState.value = CallUiState()
        resetAudio()
    }

    fun sendOffer(targetUserId: String, sdp: JSONObject) {
        callsSocketHandler.sendOffer(targetUserId, sdp)
    }

    fun sendAnswer(targetUserId: String, sdp: JSONObject) {
        callsSocketHandler.sendAnswer(targetUserId, sdp)
    }

    fun sendIceCandidate(targetUserId: String, candidate: JSONObject) {
        callsSocketHandler.sendIceCandidate(targetUserId, candidate)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onTransferOffer(sdp: JSONObject) {
        _uiState.value.targetUserId?.let { callsSocketHandler.sendOffer(it, sdp) }
    }

    override fun onTransferAnswer(sdp: JSONObject) {
        _uiState.value.targetUserId?.let { callsSocketHandler.sendAnswer(it, sdp) }
    }

    override fun onTransferIceCandidate(candidate: JSONObject) {
        _uiState.value.targetUserId?.let { callsSocketHandler.sendIceCandidate(it, candidate) }
    }

    override fun onRemoteVideoTrackReceived(videoTrack: VideoTrack) {
        _uiState.value = _uiState.value.copy(remoteVideoTrack = videoTrack)
    }

    override fun onCleared() {
        super.onCleared()
        webRtcClient.endCall()
    }
}
