package com.example.frontend.presentation.screen.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun VideoScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    onCommentClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var commentVideoId by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (uiState is VideoUiState.Loading) viewModel.load()
    }

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
                val pagerState = rememberPagerState(pageCount = { posts.size })

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage >= posts.size - 2) {
                        viewModel.loadMore()
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val isVisible = pagerState.currentPage == page
                    val post = posts[page]
                    ReelVideoItem(
                        post = post,
                        isVisible = isVisible,
                        onLikeClick = { viewModel.onLikeVideo(post.id) },
                        onCommentClick = { commentVideoId = post.id },
                        onShareClick = { viewModel.onShareVideo(post.id) },
                        onSaveClick = { viewModel.onSaveVideo(post.id) }
                    )
                }

                if (state.isLoadingMore) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                            .size(32.dp)
                    )
                }
            }
        }
    }

    if (commentVideoId != null) {
        AlertDialog(
            onDismissRequest = {
                commentVideoId = null
                commentInput = ""
            },
            title = { Text("Thêm bình luận") },
            text = {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Nhập nội dung...") },
                    minLines = 3,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    enabled = commentInput.isNotBlank(),
                    onClick = {
                        val targetVideoId = commentVideoId ?: return@TextButton
                        viewModel.onCreateVideoComment(targetVideoId, commentInput)
                        onCommentClick(targetVideoId)
                        commentVideoId = null
                        commentInput = ""
                    }
                ) {
                    Text("Gửi")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        commentVideoId = null
                        commentInput = ""
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun ReelVideoItem(
    post: Post,
    isVisible: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ReelVideoPlayer(videoUrl = post.cdnUrl, isVisible = isVisible, isMuted = isMuted)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
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
                            text = "${post.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = post.content, color = Color.White, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InteractionIcon(
                        iconRes = R.drawable.icon_hearth,
                        text = post.likeCount.toString(),
                        tint = if (post.isLiked) Color.Red else Color.White,
                        onClick = onLikeClick
                    )
                    Spacer(Modifier.height(20.dp))
                    InteractionIcon(
                        iconRes = R.drawable.icon_message,
                        text = post.commentCount.toString(),
                        onClick = onCommentClick
                    )
                    Spacer(Modifier.height(20.dp))
                    InteractionIcon(
                        iconRes = R.drawable.icon_share,
                        text = post.shareCount.toString(),
                        onClick = onShareClick
                    )
                    Spacer(Modifier.height(20.dp))
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "Save",
                        tint = if (post.isSaved) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onSaveClick)
                    )
                    Spacer(Modifier.height(20.dp))

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
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        },
                        onDoubleTap = {
                            /* TODO */
                        },
                        onTap = {
                            isPlaying = !isPlaying
                        }
                    )
                }
        )

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
                    .padding(top = 100.dp)
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
fun InteractionIcon(
    iconRes: Int,
    text: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = tint
        )
        Spacer(Modifier.height(4.dp))
        Text(text = text, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
