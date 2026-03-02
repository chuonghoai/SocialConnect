package com.example.frontend.presentation.screen.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(viewModel: VideoViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // Trigger load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    LaunchedEffect(Unit) {
        if (uiState is VideoUiState.Loading) viewModel.load()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Header luôn hiển thị trên cùng
        VideoHeader()

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.load(isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is VideoUiState.Loading -> {
                            item {
                                Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = OrangePrimary)
                                }
                            }
                        }
                        is VideoUiState.Error -> {
                            item {
                                Column(Modifier.fillMaxWidth().padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.message, color = Color.Red)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.load() }) { Text("Thử lại") }
                                }
                            }
                        }
                        is VideoUiState.Success -> {
                            items(state.posts) { post ->
                                ReelVideoItem(post = post)
                                Divider(thickness = 1.dp, color = Color.DarkGray)
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = OrangePrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Video",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "AliceApp", // Tên App
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.size(28.dp)) // Để cân bằng title ở giữa
    }
}

@Composable
fun ReelVideoItem(post: Post) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f) // Chỉnh tỷ lệ để vừa màn hình
            .background(Color.Black)
    ) {
        // 1. Trình phát Video
        ReelVideoPlayer(videoUrl = post.cdnUrl)

        // 2. Lớp phủ nội dung (Bên phải & Dưới cùng)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                // Info bên trái
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = post.userAvatar,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.icon_user)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "@${post.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = post.content, color = Color.White, fontSize = 14.sp)
                }

                // Các nút tương tác bên phải
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InteractionIcon(iconRes = R.drawable.icon_hearth, text = post.likeCount.toString())
                    Spacer(Modifier.height(16.dp))
                    InteractionIcon(iconRes = R.drawable.icon_message, text = post.commentCount.toString())
                    Spacer(Modifier.height(16.dp))
                    InteractionIcon(iconRes = R.drawable.icon_share, text = post.shareCount.toString())
                    Spacer(Modifier.height(16.dp))
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReelVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeedUp by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Quản lý trạng thái Player
    LaunchedEffect(isPlaying) { if (isPlaying) exoPlayer.play() else exoPlayer.pause() }
    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }
    LaunchedEffect(isSpeedUp) { exoPlayer.setPlaybackSpeed(if (isSpeedUp) 2f else 1f) }

    // Hủy Player khi rời khỏi màn hình
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false // Ẩn controller mặc định
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lớp xử lý thao tác vuốt / chạm vô hình
        Row(modifier = Modifier.fillMaxSize()) {
            // Nửa trái: Tua lùi 5s
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0))
                                showRewindIndicator = true
                            },
                            onTap = { isPlaying = !isPlaying }
                        )
                    }
            )

            // Nửa phải: Tua tới 5s
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration))
                                showForwardIndicator = true
                            },
                            onTap = { isPlaying = !isPlaying }
                        )
                    }
            )
        }

        // Gesture: Nhấn giữ x2 tốc độ (Bao trùm toàn màn hình để không bị chặn bởi 2 Box trên)
        Box(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            tryAwaitRelease()
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            // Nếu giữ lâu hơn 300ms thì xem như LongPress
                            if (pressDuration > 300) {
                                isSpeedUp = false // Nhả ra thì về bình thường
                            }
                        },
                        onLongPress = {
                            isSpeedUp = true // Giữ thì x2
                        }
                    )
                }
        )

        // Các Icon hiển thị phản hồi người dùng
        if (!isPlaying) {
            Icon(
                painter = painterResource(R.drawable.icon_play_video), // Bạn cần có icon play tròn
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center).size(64.dp)
            )
        }

        if (isSpeedUp) {
            Text(
                text = "x2 Tốc độ",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Nút Mute ở góc phải giữa màn hình (hoặc tùy bạn đặt)
        IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 100.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Mute",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Hiệu ứng nhấp nháy khi tua (ẩn sau 500ms)
        LaunchedEffect(showRewindIndicator) {
            if (showRewindIndicator) { delay(500); showRewindIndicator = false }
        }
        LaunchedEffect(showForwardIndicator) {
            if (showForwardIndicator) { delay(500); showForwardIndicator = false }
        }
    }
}

@Composable
fun InteractionIcon(iconRes: Int, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(text = text, color = Color.White, fontSize = 12.sp)
    }
}