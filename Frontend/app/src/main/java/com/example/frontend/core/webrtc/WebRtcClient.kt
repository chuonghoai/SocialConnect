package com.example.frontend.core.webrtc

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // EglBase dùng để render phần cứng cho Video
    val eglBaseContext: EglBase.Context by lazy { EglBase.create().eglBaseContext }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // Media Tracks
    var localVideoTrack: VideoTrack? = null
    var localAudioTrack: AudioTrack? = null

    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    // Callbacks để giao tiếp ngược lại với ViewModel
    var listener: Listener? = null

    interface Listener {
        fun onTransferOffer(sdp: JSONObject)
        fun onTransferAnswer(sdp: JSONObject)
        fun onTransferIceCandidate(candidate: JSONObject)
        fun onRemoteVideoTrackReceived(videoTrack: VideoTrack)
    }

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        val videoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }

    // 1. Khởi tạo Camera và Micro
    fun startLocalMedia(isVideoCall: Boolean, isFrontCam: Boolean = true) {
        val factory = peerConnectionFactory ?: return

        // Audio
        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)

        // Video
        if (isVideoCall) {
            videoCapturer = createVideoCapturer(isFrontCam)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            val videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30) // HD 720p 30fps

            localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
            localVideoTrack?.setEnabled(true)
        }
    }

    // 2. Khởi tạo kết nối P2P (PeerConnection)
    fun initializePeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)

        // Thêm luồng media của mình vào để gửi đi
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }
    }

    // 3. Người gọi (Caller) tạo Offer
    fun call() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { nonNullDesc -> // Kiểm tra null trước khi dùng
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), nonNullDesc)
                    val sdpJson = JSONObject().apply {
                        put("type", nonNullDesc.type.canonicalForm())
                        put("sdp", nonNullDesc.description)
                    }
                    listener?.onTransferOffer(sdpJson) // Gửi socket
                }
            }
        }, constraints)
    }

    // 4. Người nhận (Receiver) tạo Answer khi có Offer đến
    fun answer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { nonNullDesc -> // Kiểm tra null
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), nonNullDesc)
                    val sdpJson = JSONObject().apply {
                        put("type", nonNullDesc.type.canonicalForm())
                        put("sdp", nonNullDesc.description)
                    }
                    listener?.onTransferAnswer(sdpJson) // Gửi socket
                }
            }
        }, constraints)
    }

    // 5. Xử lý dữ liệu đầu vào từ Socket
    fun onRemoteSessionReceived(sdpJson: JSONObject) {
        val type = if (sdpJson.getString("type") == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        val sdp = SessionDescription(type, sdpJson.getString("sdp"))
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
    }

    fun addIceCandidate(candidateJson: JSONObject) {
        val candidate = IceCandidate(
            candidateJson.getString("sdpMid"),
            candidateJson.getInt("sdpMLineIndex"),
            candidateJson.getString("candidate")
        )
        peerConnection?.addIceCandidate(candidate)
    }

    // --- Các hàm tiện ích ---
    fun toggleAudio(mute: Boolean) { localAudioTrack?.setEnabled(!mute) }
    fun toggleVideo(mute: Boolean) { localVideoTrack?.setEnabled(!mute) }

    fun switchCamera() {
        val cameraVideoCapturer = videoCapturer as? CameraVideoCapturer
        cameraVideoCapturer?.switchCamera(null)
    }

    fun endCall() {
        videoCapturer?.tryStopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        peerConnection?.close()
        peerConnection = null
        localVideoTrack = null
        localAudioTrack = null
    }

    private fun createVideoCapturer(isFront: Boolean): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        // Tìm camera trước/sau
        for (deviceName in deviceNames) {
            if (if (isFront) enumerator.isFrontFacing(deviceName) else enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Fallback lấy cam đầu tiên tìm thấy
        return deviceNames.firstOrNull()?.let { enumerator.createCapturer(it, null) }
    }

    // Lắng nghe các sự kiện của PeerConnection
    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            val json = JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            }
            listener?.onTransferIceCandidate(json)
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                listener?.onRemoteVideoTrackReceived(track)
            }
        }

        // Các hàm bắt buộc implement nhưng để trống nếu chưa dùng
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddStream(p0: MediaStream?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
    }
}

// Lớp tiện ích để đỡ phải override nhiều hàm rỗng của SdpObserver
open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}

private fun VideoCapturer.tryStopCapture() {
    try { stopCapture() } catch (e: Exception) { Log.e("WebRtcClient", "Error stopping capture", e) }
}
