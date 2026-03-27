package com.example.frontend.presentation.screen.chat

import MessageItem
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.ui.component.MediaViewerDialog

@Composable
fun ChatScreen(
    conversationId: String,
    partnerId: String,
    conversationName: String = "Người dùng",
    conversationAvatarUrl: String? = null,
    onBackClick: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var viewerMediaList by remember { mutableStateOf<List<PostMedia>?>(null) }
    var viewerInitialPage by remember { mutableIntStateOf(0) }
    if (viewerMediaList != null) {
        MediaViewerDialog(
            mediaItems = viewerMediaList!!,
            initialPage = viewerInitialPage,
            onDismiss = { viewerMediaList = null }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            uris.forEach { viewModel.onMediaSelected(it) }
        }
    )

    // Permission launcher for Recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleVoiceRecorder(true)
            } else {
                Log.e("ChatScreen", "Quyền RECORD_AUDIO bị từ chối")
            }
        }
    )

    val isPartnerOnline = uiState.onlineUsers.contains(partnerId)

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE9E9E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 66.dp)
                .consumeWindowInsets(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = 10.dp,
                            start = 12.dp,
                            end = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        reverseLayout = true
                    ) {
                        item {
                            Column {
                                AnimatedVisibility(
                                    visible = uiState.isPartnerTyping,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$conversationName đang nhập...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 42.dp)
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { index, msg ->
                            val isMine = msg.sender.id == uiState.currentUser?.id
                            
                            val statusText = if (isMine && index == 0) {
                                when {
                                    msg.id.startsWith("temp_voice_") -> "• Đang gửi tin nhắn thoại"
                                    msg.id.startsWith("temp_media_") -> "• Đang tải phương tiện"
                                    msg.id.startsWith("temp_") -> "• Đang gửi"
                                    msg.id.startsWith("failed_") -> "• Lỗi gửi"
                                    msg.isRead -> "Đã đọc"
                                    else -> "• Đã gửi"
                                }
                            } else null

                            MessageBubble(
                                message = msg,
                                isMine = isMine,
                                incomingAvatarUrl = conversationAvatarUrl,
                                statusText = statusText,
                                onMediaClick = { selectedMedia ->
                                    viewerMediaList = listOf(selectedMedia)
                                    viewerInitialPage = 0
                                }
                            )
                        }
                    }
                }
            }

            // Thanh preview media đã chọn
            if (uiState.selectedMedia.isNotEmpty() && !uiState.showVoiceRecorder) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedMedia) { uri ->
                        Box(modifier = Modifier.size(60.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .alpha(0.7f),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                            )
                            IconButton(
                                onClick = { viewModel.onMediaSelected(uri) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopStart)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.showVoiceRecorder) {
                VoiceRecorderUI(
                    uiState = uiState,
                    onStartRecording = { 
                        Log.d("ChatFlow", "Bắt đầu nhấn giữ -> startRecording")
                        viewModel.startRecording() 
                    },
                    onStopRecording = { 
                        Log.d("ChatFlow", "Thả tay -> stopRecording")
                        viewModel.stopRecording() 
                    },
                    onCancel = { 
                        Log.d("ChatFlow", "Bấm hủy/xóa -> toggleVoiceRecorder(false)")
                        viewModel.toggleVoiceRecorder(false) 
                    },
                    onSend = { 
                        Log.d("ChatFlow", "Bấm gửi -> sendVoiceMessage")
                        viewModel.sendVoiceMessage(conversationId) 
                    }
                )
            } else {
                ChatInputBar(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        viewModel.onTyping(it)
                    },
                    onSendClick = {
                        if (messageText.isNotBlank() || uiState.selectedMedia.isNotEmpty()) {
                            viewModel.sendChatWithMedia(conversationId, messageText)
                            messageText = ""
                        }
                    },
                    onAddClick = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    onVoiceClick = { 
                        Log.d("ChatFlow", "Bấm icon micro -> Kiểm tra quyền")
                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.toggleVoiceRecorder(true)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isSendEnabled = messageText.isNotBlank() || uiState.selectedMedia.isNotEmpty()
                )
            }
        }

        ChatTopBar(
            title = conversationName,
            avatarUrl = conversationAvatarUrl,
            isOnline = isPartnerOnline,
            onBackClick = onBackClick,
            onVoiceCallClick = onVoiceCallClick,
            onVideoCallClick = onVideoCallClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )
    }
}

@Composable
fun VoiceRecorderUI(
    uiState: ChatUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    var isPlayingPreview by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isPlayingPreview) {
        if (isPlayingPreview) {
            while (isPlayingPreview && mediaPlayer?.isPlaying == true) {
                playbackPosition = mediaPlayer?.currentPosition?.toFloat() ?: 0f
                delay(50)
            }
            if (mediaPlayer?.isPlaying == false) isPlayingPreview = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.recordingFileUri == null) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isRecording) OrangePrimary.copy(alpha = 0.2f) else Color(0xFFF1F1F1))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onStartRecording()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onStopRecording()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardVoice,
                    contentDescription = null,
                    tint = if (uiState.isRecording) OrangePrimary else Color.Gray,
                    modifier = Modifier.size(40.dp).let { 
                        if (uiState.isRecording) it.graphicsLayer(scaleX = scale, scaleY = scale) else it 
                    }
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.isRecording) formatDuration(uiState.recordingDuration) else "Nhấn giữ để thu âm",
                color = if (uiState.isRecording) OrangePrimary else Color.Gray,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        } else {
            // Hiệu ứng Review sau khi thu xong
            LaunchedEffect(uiState.recordingFileUri) {
                if (uiState.recordingFileUri != null) {
                    try {
                        val tempPlayer = MediaPlayer.create(context, uiState.recordingFileUri)
                        duration = tempPlayer?.duration?.toLong() ?: 0L
                        Log.d("ChatFlow", "Review mode: File URI = ${uiState.recordingFileUri}, Duration = $duration ms")
                        tempPlayer?.release()
                    } catch (e: Exception) {
                        Log.e("ChatFlow", "Lỗi khi lấy duration file ghi âm: ${e.message}")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isPlayingPreview) {
                        mediaPlayer?.pause()
                        isPlayingPreview = false
                    } else {
                        try {
                            if (mediaPlayer == null) {
                                mediaPlayer = MediaPlayer.create(context, uiState.recordingFileUri)
                                duration = mediaPlayer?.duration?.toLong() ?: 0L
                            }
                            mediaPlayer?.start()
                            isPlayingPreview = true
                        } catch (e: Exception) {
                            Log.e("ChatFlow", "Lỗi khi phát thử: ${e.message}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = OrangePrimary
                    )
                }

                Slider(
                    value = playbackPosition,
                    onValueChange = { 
                        playbackPosition = it
                        mediaPlayer?.seekTo(it.toInt())
                    },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = OrangePrimary, activeTrackColor = OrangePrimary)
                )
                
                Text(
                    text = formatDuration(if (isPlayingPreview) playbackPosition.toLong() else duration),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(30.dp))
                }

                Button(
                    onClick = {
                        Log.d("ChatFlow", "Bắt đầu gửi voice: URI = ${uiState.recordingFileUri}")
                        onSend()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun MessageBubble(
    message: MessageItem,
    isMine: Boolean,
    incomingAvatarUrl: String?,
    statusText: String? = null,
    onMediaClick: (PostMedia) -> Unit
) {
    val isUploading = message.id.startsWith("temp_")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine) {
                AsyncImage(
                    model = message.sender.avatarUrl ?: incomingAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.icon_user),
                    error = painterResource(id = R.drawable.icon_user)
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 240.dp),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                val isAudioMessage = message.type == "AUDIO" || message.media.firstOrNull()?.type == "AUDIO"
                if (isAudioMessage) {
                    AudioMessageBubble(message, isMine, isUploading)
                } else {
                    message.media.forEach { media ->
                        val postMedia = PostMedia(
                            cdnUrl = media.secureUrl,
                            kind = if (media.type == "VIDEO") "VIDEO" else "IMAGE"
                        )

                        if (media.type == "VIDEO") {
                            Box(modifier = Modifier.clickable { onMediaClick(postMedia) }) {
                                VideoMessageBubble(media.secureUrl, isUploading)
                            }
                        } else if (media.type == "IMAGE") {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                                    .clickable { onMediaClick(postMedia) }
                            ) {
                                AsyncImage(
                                    model = media.secureUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .heightIn(max = 300.dp)
                                        .alpha(if (isUploading) 0.5f else 1f),
                                    contentScale = ContentScale.Crop
                                )
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .align(Alignment.Center),
                                        color = OrangePrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }

                    if (message.text.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMine) Color(0xFFE8A46F) else Color(0xFFF4F4F4),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = message.text,
                                color = Color(0xFF1F1F1F),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.padding(
                top = 2.dp,
                start = if (isMine) 0.dp else 42.dp,
                end = if (isMine) 42.dp else 0.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMessageTime(message.createAt),
                color = Color(0xFF8A8A8A),
                fontSize = 10.sp
            )
            
            if (statusText != null) {
                Spacer(Modifier.width(4.dp))
                if (statusText == "Đã đọc") {
                    Icon(Icons.Default.DoneAll, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    text = statusText,
                    color = if (statusText.contains("Đang") || statusText.contains("Lỗi")) Color.Gray else OrangePrimary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AudioMessageBubble(message: MessageItem, isMine: Boolean, isUploading: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPos by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    val audioUrl = message.media.firstOrNull()?.secureUrl ?: ""

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                currentPos = mediaPlayer?.currentPosition ?: 0
                delay(100)
            }
            if (mediaPlayer?.isPlaying == false) isPlaying = false
        }
    }

    Surface(
        modifier = Modifier
            .width(200.dp)
            .clickable {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepareAsync()
                            setOnPreparedListener { 
                                duration = it.duration
                                it.start()
                                isPlaying = true
                            }
                            setOnCompletionListener { 
                                isPlaying = false 
                                currentPos = 0
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isMine) Color(0xFFE8A46F) else Color(0xFFF4F4F4),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isMine) Color.White else OrangePrimary
            )
            
            Spacer(Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPos.toFloat() / duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (isMine) Color.White else OrangePrimary,
                    trackColor = (if (isMine) Color.White else OrangePrimary).copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isPlaying) formatDuration((duration - currentPos).toLong()) else "Voice Note",
                    fontSize = 10.sp,
                    color = if (isMine) Color.White else Color.Gray
                )
            }
            
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            }
        }
    }
}

@Composable
fun VideoMessageBubble(url: String, isUploading: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .width(200.dp)
            .heightIn(max = 300.dp, min = 150.dp)
    ) {
        if (isPlaying && !isUploading) {
            val context = LocalContext.current
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    exoPlayer.release()
                }
            }
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val thumbnailUrl = if (url.contains("cloudinary.com")) {
                url.substringBeforeLast(".") + ".jpg"
            } else url

            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video Thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isUploading) 0.5f else 1f),
                contentScale = ContentScale.Crop
            )

            if (!isUploading) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .clickable { isPlaying = true }
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center),
                    color = OrangePrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

fun formatMessageTime(isoString: String): String {
    return try {
        val sdf = if (isoString.contains("Z")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        }
        val date = sdf.parse(isoString)
        val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        outSdf.format(date!!)
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onBackClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(Color.White)
            .border(width = 0.8.dp, color = Color(0xFFD3D3D3))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF212121),
                modifier = Modifier.size(24.dp)
            )
        }

        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.icon_user),
            error = painterResource(id = R.drawable.icon_user)
        )

        Box(
            modifier = Modifier
                .offset(x = (-4).dp, y = 10.dp)
                .size(8.dp)
                .background(if (isOnline) OnlineGreen else Color.Gray, CircleShape)
                .border(1.dp, Color.White, CircleShape)
        )

        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF111111),
                fontSize = 25.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                color = Color(0xFF6F6F6F),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        IconButton(onClick = onVoiceCallClick) {
            Icon(Icons.Default.Phone, null, tint = Color(0xFF212121), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onVideoCallClick) {
            Icon(Icons.Default.Videocam, null, tint = Color(0xFF212121), modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isSendEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 0.8.dp, color = Color(0xFFD9D9D9))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onVoiceClick,
            modifier = Modifier.size(34.dp).background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardVoice, null, tint = Color(0xFF5A5A5A), modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(6.dp))

        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(34.dp).background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(Icons.Default.Add, null, tint = Color(0xFF5A5A5A), modifier = Modifier.size(21.dp))
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE9E9E9))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color(0xFF333333), fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(OrangePrimary),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(1.dp).height(22.dp).background(Color(0xFFE1A66E)))
                        Spacer(Modifier.width(6.dp))
                        innerTextField()
                    }
                }
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { if (isSendEnabled) onSendClick() },
            modifier = Modifier.size(34.dp).background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = if (isSendEnabled) OrangePrimary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
