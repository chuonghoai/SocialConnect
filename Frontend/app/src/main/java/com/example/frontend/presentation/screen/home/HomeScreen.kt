package com.example.frontend.presentation.screen.home

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User
import com.example.frontend.ui.theme.OrangePrimary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    currentUser: User?,
    onPostClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    // Tự động load dữ liệu lần đầu
    LaunchedEffect(Unit) {
        if (uiState is HomeUiState.Loading) {
            viewModel.load()
        }
    }

    Scaffold(
        topBar = { HomeTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = Color.Red)
                            Button(onClick = { viewModel.load() }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    val posts = state.posts
                    // Xử lý Pull to Refresh
                    PullToRefreshBox(
                        state = pullRefreshState,
                        isRefreshing = false, // Có thể quản lý state refreshing riêng nếu muốn
                        onRefresh = { viewModel.load() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // Phần tạo bài viết mới
                            item {
                                if (currentUser != null) {
                                    CreatePostSection(currentUser)
                                }
                            }

                            // Danh sách bài viết
                            items(posts) { post ->
                                PostCard(
                                    post = post,
                                    onClick = {
                                        viewModel.selectPost(post)
                                        onPostClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTopBar() {
    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .height(60.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AliceApp",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                style = MaterialTheme.typography.headlineMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Action New Post */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_plus),
                        contentDescription = "New Post",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { /* Action Messages */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_message),
                        contentDescription = "Messages",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePostSection(user: User) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user) // Ảnh mặc định nếu lỗi
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Bạn đang nghĩ gì?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_image),
                contentDescription = "Add Image",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PostCard(post: Post, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape // Facebook style thường là hình chữ nhật full width
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar + Tên + Thời gian
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.userAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.displayName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_earth),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatTimeAgo(post.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Content: Text
            Text(
                text = post.content,
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Content: Media (Ảnh/Video)
            if (post.cdnUrl.isNotEmpty()) {
                PostMediaContent(post)
            }

            Spacer(Modifier.height(12.dp))

            // Footer: Like/Comment/Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LikeButton(initialCount = post.likeCount)
                Spacer(Modifier.width(24.dp))
                InteractionItem(R.drawable.icon_message, post.commentCount.toString(), onClick = onClick)
                Spacer(Modifier.width(24.dp))
                InteractionItem(R.drawable.icon_share, post.shareCount.toString())
            }
        }
    }
}

@Composable
fun PostMediaContent(post: Post) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .background(Color.Black) // Nền đen cho video
    ) {
        if (post.kind == "IMAGE") {
            AsyncImage(
                model = post.cdnUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        } else if (post.kind == "VIDEO") {
            // Gọi component Video Player tùy biến
            ExoVideoPlayer(videoUrl = post.cdnUrl)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Khởi tạo ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false // Không tự động play để tránh ồn ào khi lướt feed
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // State quản lý UI
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) } // Mặc định hiện controls
    var isSpeedUp by remember { mutableStateOf(false) } // Trạng thái đang tua nhanh x2
    var showSeekOverlay by remember { mutableStateOf<String?>(null) } // +5s hoặc -5s

    // 2. Quản lý vòng đời (Pause khi app background, Release khi lướt qua)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // 3. Listener cập nhật state từ Player -> Compose
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // 4. Vòng lặp cập nhật thời gian chạy (progress bar)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentTime = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(500) // Cập nhật mỗi 0.5s
        }
    }

    // Logic ẩn controls sau 3s nếu không thao tác
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f) // Tỉ lệ khung hình (có thể sửa thành dynamic nếu cần)
    ) {
        // A. Render Video Surface
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Tắt UI mặc định của ExoPlayer
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // B. Lớp phủ nhận diện cử chỉ (Gestures Overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // Logic Double Tap: Tua nhanh/lùi 5s
                            val screenWidth = size.width
                            val isForward = offset.x > screenWidth / 2
                            val seekAmount = 5000L // 5 giây

                            if (isForward) {
                                exoPlayer.seekTo(exoPlayer.currentPosition + seekAmount)
                                showSeekOverlay = "+5s"
                            } else {
                                exoPlayer.seekTo(exoPlayer.currentPosition - seekAmount)
                                showSeekOverlay = "-5s"
                            }
                            // Reset overlay sau 1s
                            currentTime = exoPlayer.currentPosition // Update UI ngay
                        },
                        onTap = {
                            isControlsVisible = !isControlsVisible
                        },
                        onPress = {
                            // Logic Nhấn giữ (Long Press): Speed x2
                            // Sự kiện onPress chạy ngay khi chạm vào
                            // Ta dùng tryAwaitRelease để biết khi nào thả tay ra
                            val pressStartTime = System.currentTimeMillis()

                            try {
                                // Đợi một chút để phân biệt với tap thường (optional)
                                if (tryAwaitRelease()) {
                                    // Đã thả tay ra (đây là thao tác Click/Tap bình thường)
                                } else {
                                    // User đang giữ tay -> Set speed x2
                                    // (Lưu ý: tryAwaitRelease trả về false nếu bị cancel hoặc timeout,
                                    // nhưng ở đây ta setup logic đơn giản hơn ở dưới)
                                }
                            } finally {
                                // Khi thả tay: Reset speed
                                exoPlayer.setPlaybackSpeed(1f)
                                isSpeedUp = false
                            }
                        }
                    )
                }
                // Xử lý riêng phần Long Press để mượt mà hơn vì detectTapGestures ở trên đôi khi bị conflict
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent().changes.firstOrNull()
                            if (down != null && down.pressed) {
                                // Khi nhấn xuống: Đợi 500ms xem có phải là giữ không
                                try {
                                    withTimeout(500) {
                                        // Chờ xem có nhấc lên không
                                        val up = awaitPointerEvent()
                                    }
                                    // Nếu nhấc lên sớm -> Tap, không làm gì ở đây
                                } catch (e: Exception) {
                                    // Timeout -> Đã giữ quá 500ms -> Speed x2
                                    isSpeedUp = true
                                    exoPlayer.setPlaybackSpeed(2.0f)
                                    // Chờ cho đến khi thả ra
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })

                                    // Thả ra -> Reset
                                    exoPlayer.setPlaybackSpeed(1.0f)
                                    isSpeedUp = false
                                }
                            }
                        }
                    }
                }
        ) {
            // C. Overlay hiển thị "+5s" / "-5s" / "2x Speed"
            if (showSeekOverlay != null) {
                LaunchedEffect(showSeekOverlay) {
                    delay(800)
                    showSeekOverlay = null
                }
                Box(
                    modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(0.5f), CircleShape).padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (showSeekOverlay == "+5s") Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(text = showSeekOverlay!!, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isSpeedUp) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Speed 2x >>", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // D. UI Controls (Play/Pause, Seekbar, Mute...)
            if (isControlsVisible) {
                // Dim background nhẹ để icon nổi bật
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                // 1. Center Play/Pause Button
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 2. Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Time & Mute Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Current / Duration
                        Text(
                            text = "${formatVideoTime(currentTime)} / ${formatVideoTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Mute Button
                        IconButton(onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }
                    }

                    // Seek Bar (Slider)
                    Slider(
                        value = currentTime.toFloat(),
                        onValueChange = {
                            currentTime = it.toLong() // Update UI mượt mà khi kéo
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(currentTime) // Seek thật khi thả tay
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = OrangePrimary,
                            activeTrackColor = OrangePrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
        }
    }
}

fun formatVideoTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun LikeButton(initialCount: Int) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(initialCount) }

    // Scale animation: bật bật nẩy khi like
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "likeScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            isLiked = !isLiked
            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
        }
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Like",
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = likeCount.toString(),
            fontSize = 12.sp,
            color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InteractionItem(iconRes: Int, count: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatTimeAgo(timeString: String): String {
    return try {
        // Giả sử định dạng server trả về là chuẩn ISO hoặc tương tự.
        // Cần điều chỉnh formatter tùy theo backend của bạn.
        // Ví dụ này dùng logic đơn giản, bạn có thể thay thế bằng thư viện PrettyTime
        val now = LocalDateTime.now()
        // Lưu ý: Cần parse đúng định dạng string từ backend.
        // Ở đây demo trả về string gốc hoặc xử lý đơn giản
        timeString.substring(0, 10) // Lấy ngày tháng năm tạm
    } catch (e: Exception) {
        "Vừa xong"
    }
}