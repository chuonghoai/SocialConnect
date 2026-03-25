package com.example.frontend.presentation.screen.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import org.webrtc.EglBase

val BgColor = Color(0xFF1C1C1E)
val TextSubColor = Color(0xFFD1D1D6)
val DangerColor = Color(0xFFFF3B30)
val SuccessColor = Color(0xFF34C759)
val ButtonBgColor = Color(0x33FFFFFF)

@Composable
fun CallScreen(
    status: String,
    fullname: String,
    avatarUrl: String,
    isVideoCall: Boolean,
    isIncoming: Boolean,
    formattedDuration: String,
    isMicOn: Boolean,
    isCamOn: Boolean,
    isSpeakerOn: Boolean,
    localVideoTrack: VideoTrack? = null,
    remoteVideoTrack: VideoTrack? = null,
    eglBaseContext: EglBase.Context,
    onToggleMic: () -> Unit,
    onToggleCam: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current

    // 1. Khởi tạo danh sách quyền cần thiết dựa trên loại cuộc gọi
    val requiredPermissions = if (isVideoCall) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    // 2. Kiểm tra trạng thái quyền hiện tại
    var hasAllPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Biến để hiển thị thông báo nếu người dùng từ chối quyền
    var permissionDenied by remember { mutableStateOf(false) }

    // 3. Khai báo Launcher để bung popup xin quyền
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Kiểm tra xem tất cả các quyền được yêu cầu đã được cấp chưa
        val allGranted = requiredPermissions.all { permissionsMap[it] == true }
        hasAllPermissions = allGranted
        permissionDenied = !allGranted

        if (!allGranted) {
             onEndCall()
        }
    }

    // 4. Tự động yêu cầu quyền khi mở màn hình (nếu chưa có)
    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // 5. Xử lý UI dựa trên trạng thái quyền
    if (!hasAllPermissions) {
        // Giao diện chờ cấp quyền hoặc khi bị từ chối
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (permissionDenied) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerColor, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Không thể thực hiện cuộc gọi.\nBạn đã từ chối quyền truy cập Micro/Camera.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
                    ) {
                        Text("Quay lại", color = Color.White)
                    }
                } else {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang yêu cầu quyền truy cập...", color = TextSubColor)
                }
            }
        }
        return
    }

    val getStatusText = {
        when (status) {
            "connecting" -> "Đang kết nối..."
            "ringing" -> "Đang đổ chuông..."
            "rejected" -> "Đã từ chối cuộc gọi"
            "ended" -> "Cuộc gọi đã kết thúc"
            else -> formattedDuration
        }
    }
    val isAccepted = status == "accepted"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 1. BACKGROUND VIDEO (Đối phương) - Chỉ hiện khi Video Call VÀ đã Accept
        if (isVideoCall && isAccepted) {
            if (remoteVideoTrack != null) {
                WebRTCVideoView(
                    videoTrack = remoteVideoTrack,
                    isLocal = false,
                    eglBaseContext = eglBaseContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Đang kết nối video...", color = Color.White)
                }
            }
        }

        // 2. GIAO DIỆN NGƯỜI NHẬN - LÚC ĐANG ĐỔ CHUÔNG (HOẶC TỪ CHỐI)
        if (isIncoming && (status == "ringing" || status == "rejected")) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallAvatar(avatarUrl)
                Text(
                    text = fullname,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (status == "rejected") "Đã từ chối cuộc gọi" else if (isVideoCall) "Cuộc gọi Video đến" else "Cuộc gọi Thoại đến",
                    color = TextSubColor,
                    fontSize = 16.sp
                )
            }

            if (status == "ringing") {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallActionButton(
                        icon = Icons.Filled.PhoneMissed,
                        color = DangerColor,
                        text = "Từ chối",
                        onClick = onRejectCall
                    )
                    CallActionButton(
                        icon = Icons.Filled.Call,
                        color = SuccessColor,
                        text = "Chấp nhận",
                        onClick = onAcceptCall
                    )
                }
            }
            return@Box // Dừng vẽ các thành phần khác nếu đang ở màn hình gọi đến
        }

        // 3. THÔNG TIN NGƯỜI GỌI (Avatar, Tên) KHI CHƯA NHẤC MÁY HOẶC CHỈ GỌI THOẠI
        if (!isVideoCall || !isAccepted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallAvatar(avatarUrl)
                Text(
                    text = fullname,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = getStatusText(),
                    color = TextSubColor,
                    fontSize = 16.sp
                )
            }
        }

        // 4. THÔNG TIN NHỎ GÓC TRÊN KHI ĐANG CALL VIDEO (Đã nhấc máy)
        if (isVideoCall && isAccepted) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 60.dp, start = 20.dp)
            ) {
                Text(
                    text = fullname,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getStatusText(),
                    color = Color.White
                )
            }
        }

        // 5. LOCAL CAMERA (Camera của mình - Phải đè lên trên)
        if (isVideoCall && isCamOn && localVideoTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 20.dp)
                    .size(width = 100.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3A3A3C))
                    .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(12.dp))
                    .shadow(5.dp)
            ) {
                WebRTCVideoView(
                    videoTrack = localVideoTrack,
                    isLocal = true,
                    eglBaseContext = eglBaseContext,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 6. CÁC NÚT ĐIỀU KHIỂN DƯỚI ĐÁY
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 50.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                isActive = isMicOn,
                onClick = onToggleMic
            )
            ControlButton(
                icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )

            if (isVideoCall) {
                ControlButton(
                    icon = if (isCamOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    isActive = isCamOn,
                    onClick = onToggleCam
                )
                ControlButton(
                    icon = Icons.Filled.FlipCameraAndroid,
                    isActive = false, // Nút lật cam thường không có state active
                    onClick = onSwitchCamera
                )
            }

            // Nút kết thúc
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(DangerColor)
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// --- Các Composable Component Phụ ---

@Composable
fun CallAvatar(url: String) {
    AsyncImage(
        model = url,
        contentDescription = "Avatar",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(120.dp)
            .padding(bottom = 20.dp)
            .clip(CircleShape)
            .border(2.dp, Color(0x33FFFFFF), CircleShape)
    )
}

@Composable
fun CallActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (isActive) ButtonBgColor else Color.White)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else BgColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// --- Trình render WebRTC dùng AndroidView ---

@Composable
fun WebRTCVideoView(
    videoTrack: VideoTrack,
    isLocal: Boolean,
    eglBaseContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                if (isLocal) {
                    setMirror(true)
                    setZOrderMediaOverlay(true)
                }
            }
        },
        update = { renderer ->
            videoTrack.addSink(renderer)
        },
        modifier = modifier
    )
}