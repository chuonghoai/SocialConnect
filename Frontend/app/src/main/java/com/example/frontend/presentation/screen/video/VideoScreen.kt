package com.example.frontend.presentation.screen.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.annotation.OptIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(viewModel: VideoViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState is VideoUiState.Loading) viewModel.load()
    }

    // Box ngoài cùng chứa toàn bộ màn hình
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is VideoUiState.Loading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is VideoUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = Color.Red)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Thử lại") }
                }
            }
            is VideoUiState.Success -> {
                val posts = state.posts
                // Khởi tạo trạng thái của Pager (mỗi trang là 1 video)
                val pagerState = rememberPagerState(pageCount = { posts.size })

                // Tự động load thêm khi cuộn gần cuối danh sách (cách 2 video)
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage >= posts.size - 2) {
                        viewModel.loadMore()
                    }
                }

                // 1. VerticalPager: Giải quyết yêu cầu tự nhảy từng item và phủ kín
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Kiểm tra xem video này có đang hiển thị trên màn hình không
                    val isVisible = pagerState.currentPage == page
                    ReelVideoItem(post = posts[page], isVisible = isVisible)
                }

                if (state.isLoadingMore) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp) // Né Bottom bar
                            .size(32.dp)
                    )
                }
            }
        }

        // 2. Video Header: Đặt ở đây (sau VerticalPager) để nó nổi (overlay) đè lên trên Video
        VideoHeader(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun VideoHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Gradient trong suốt: Từ đen mờ ở trên cùng chuyển dần sang trong suốt ở dưới
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
            // Padding status bar để thanh trạng thái điện thoại (giờ/pin) không đè vào chữ
            .statusBarsPadding(),
//            .padding(horizontal = 16.dp, vertical = 12.dp),
//            .height(50.dp),
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
            text = "AliceApp",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.size(28.dp)) // Spacer rỗng để cân bằng title ở giữa
    }
}

@Composable
fun ReelVideoItem(post: Post, isVisible: Boolean) {
    // ĐẨY STATE LÊN ĐÂY: Quản lý trạng thái âm thanh ngay tại Item
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Trình phát Video (Truyền isMuted vào)
        ReelVideoPlayer(videoUrl = post.cdnUrl, isVisible = isVisible, isMuted = isMuted)

        // Lớp phủ nội dung (Info & Nút tương tác)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                // Nội dung Text bên trái
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = post.userAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
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
                    Spacer(Modifier.height(12.dp))
                    Text(text = post.content, color = Color.White, fontSize = 14.sp)
                }

                // Các Icon tương tác bên phải (ĐÃ ĐƯỢC GOM CHUNG)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InteractionIcon(iconRes = R.drawable.icon_hearth, text = post.likeCount.toString())
                    Spacer(Modifier.height(20.dp))
                    InteractionIcon(iconRes = R.drawable.icon_message, text = post.commentCount.toString())
                    Spacer(Modifier.height(20.dp))
                    InteractionIcon(iconRes = R.drawable.icon_share, text = post.shareCount.toString())
                    Spacer(Modifier.height(20.dp))
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(20.dp))

                    // NÚT MUTE ĐÃ ĐƯỢC CHUYỂN VÀO ĐÂY
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ReelVideoPlayer(
    videoUrl: String,
    isVisible: Boolean,
    isMuted: Boolean
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isSpeedUp by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Tối ưu hóa: CHỈ phát video nếu màn hình này đang được focus (isVisible == true)
    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }
    LaunchedEffect(isSpeedUp) { exoPlayer.setPlaybackSpeed(if (isSpeedUp) 2f else 1f) }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    // SỬA Ở ĐÂY: Thuộc tính này cực kỳ quan trọng.
                    // Nó sẽ "zoom" video lên để cắt bỏ viền đen, lấp đầy 100% diện tích màn hình.
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize() // Đảm bảo View chiếm toàn bộ
        )

        // Lớp xử lý thao tác vuốt / chạm vô hình
        // ĐÃ XÓA Row() VÀ Box() CŨ, THAY BẰNG 1 BOX DUY NHẤT DƯỚI ĐÂY:
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            tryAwaitRelease()
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            // Nhả tay ra thì tắt chế độ x2
                            if (pressDuration > 300) {
                                isSpeedUp = false
                            }
                        },
                        onLongPress = {
                            // Đè lâu thì bật x2
                            isSpeedUp = true
                        },
                        onDoubleTap = { offset ->
                            // Lấy chiều rộng màn hình để chia đôi
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                // Double tap nửa TRÁI -> Tua LẠI
                                exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0))
                                showRewindIndicator = true
                            } else {
                                // Double tap nửa PHẢI -> Tua TỚI
                                exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration))
                                showForwardIndicator = true
                            }
                        },
                        onTap = {
                            // Chạm 1 lần -> Play/Pause
                            isPlaying = !isPlaying
                        }
                    )
                }
        )

        Box(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            tryAwaitRelease()
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            if (pressDuration > 300) {
                                isSpeedUp = false
                            }
                        },
                        onLongPress = {
                            isSpeedUp = true
                        }
                    )
                }
        )

        // Icon báo hiệu
        if (!isPlaying) {
            Icon(
                painter = painterResource(R.drawable.icon_play_video),
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
                    .padding(top = 100.dp) // Dịch xuống tránh lẹm vào Header
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

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
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}