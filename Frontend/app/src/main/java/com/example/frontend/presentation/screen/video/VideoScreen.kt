package com.example.frontend.presentation.screen.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.ui.component.PostMediaPreview
import kotlinx.coroutines.delay
import kotlin.OptIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    onCommentClick: (String) -> Unit = {},
    currentUserAvatarUrl: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val commentsSheetState by viewModel.commentsSheetState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        viewModel.onMediaSelected(uris)
    }

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
                    Button(onClick = { viewModel.load() }) { Text("Thu lai") }
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
                        onCommentClick = {
                            viewModel.openComments(post.id)
                            onCommentClick(post.id)
                        },
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

    if (commentsSheetState.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeComments() },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            VideoCommentsSheet(
                state = commentsSheetState,
                currentUserAvatarUrl = currentUserAvatarUrl,
                onInputChange = viewModel::onCommentInputChange,
                onSend = viewModel::submitVideoComment,
                onAttachClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onReplyClick = viewModel::onReplyToComment,
                onCancelReply = viewModel::cancelReply,
                onRemoveSelectedMedia = viewModel::removeSelectedMedia,
                onClearSelectedMedia = viewModel::clearSelectedMedia
            )
        }
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
                            text = post.displayName,
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
        if (isVisible && isPlaying) exoPlayer.play() else exoPlayer.pause()
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
                            if (pressDuration > 300) isSpeedUp = false
                        },
                        onLongPress = { isSpeedUp = true },
                        onDoubleTap = { },
                        onTap = { isPlaying = !isPlaying }
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
                text = "x2 toc do",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        LaunchedEffect(showRewindIndicator) {
            if (showRewindIndicator) {
                delay(500)
                showRewindIndicator = false
            }
        }
        LaunchedEffect(showForwardIndicator) {
            if (showForwardIndicator) {
                delay(500)
                showForwardIndicator = false
            }
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

private data class CommentUiItem(
    val comment: Comment,
    val level: Int
)

private fun buildCommentTree(comments: List<Comment>): List<CommentUiItem> {
    if (comments.isEmpty()) return emptyList()

    val byParent = comments.groupBy { it.parentCommentId }
    val roots = byParent[null].orEmpty().sortedBy { it.createdAt }
    val result = mutableListOf<CommentUiItem>()

    fun dfs(node: Comment, level: Int) {
        result.add(CommentUiItem(node, level))
        val children = byParent[node.id].orEmpty().sortedBy { it.createdAt }
        children.forEach { child -> dfs(child, level + 1) }
    }

    roots.forEach { root -> dfs(root, 0) }

    // Keep orphan replies visible even if parent not in current page.
    val rootIds = roots.map { it.id }.toSet()
    val orphans = comments.filter { it.parentCommentId != null && it.parentCommentId !in rootIds && it !in roots }
    orphans.sortedBy { it.createdAt }.forEach { orphan ->
        if (result.none { it.comment.id == orphan.id }) result.add(CommentUiItem(orphan, 0))
    }

    return result
}

private fun resolveCommentMedia(comment: Comment): List<PostMedia> {
    val fromServer = comment.media
        .mapNotNull { mediaItem ->
            val url = mediaItem.resolvedUrl().trim()
            if (url.isBlank()) null else mediaItem.copy(cdnUrl = url)
        }
        .distinctBy { mediaItem -> mediaItem.resolvedUrl().trim() }
    if (fromServer.isNotEmpty()) return fromServer

    val fallbackUrls = listOf(comment.mediaUrl?.trim().orEmpty())
        .filter { it.isNotBlank() }
        .distinct()
    if (fallbackUrls.isEmpty()) return emptyList()

    return fallbackUrls.map { url ->
        PostMedia(
            cdnUrl = url,
            kind = comment.mediaType ?: ""
        )
    }
}

@Composable
private fun VideoCommentsSheet(
    state: VideoCommentsSheetState,
    currentUserAvatarUrl: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onReplyClick: (Comment) -> Unit,
    onCancelReply: () -> Unit,
    onRemoveSelectedMedia: (Uri) -> Unit,
    onClearSelectedMedia: () -> Unit
) {
    val commentItems = remember(state.comments) { buildCommentTree(state.comments) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 620.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Binh luan",
            color = Color.Black,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Black)
                }
            }

            commentItems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage ?: "Chua co binh luan",
                        color = Color.DarkGray
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(commentItems, key = { it.comment.id }) { item ->
                        val comment = item.comment
                        val startPad = 8.dp + (item.level * 16).dp
                        val commentMedia = remember(comment.media, comment.mediaUrl, comment.mediaType) {
                            resolveCommentMedia(comment)
                        }

                        Column(modifier = Modifier.padding(start = startPad)) {
                            Row(verticalAlignment = Alignment.Top) {
                                AsyncImage(
                                    model = comment.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.icon_user)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.displayName,
                                        color = Color.Black,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    if (comment.content.isNotBlank()) {
                                        Text(
                                            text = comment.content,
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                    if (commentMedia.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        PostMediaPreview(mediaItems = commentMedia)
                                    }
                                    TextButton(
                                        onClick = { onReplyClick(comment) },
                                        modifier = Modifier.padding(start = 0.dp)
                                    ) {
                                        Text("Tra loi", color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.replyingToComment != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dang tra loi ${state.replyingToComment.displayName}",
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel reply",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp).clickable(onClick = onCancelReply)
                )
            }
        }

        if (state.selectedMediaUris.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.selectedMediaUris) { uri ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.icon_image)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Tep da chon",
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove media",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(18.dp).clickable { onRemoveSelectedMedia(uri) }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear all media",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp).clickable(onClick = onClearSelectedMedia)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentUserAvatarUrl,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = state.commentInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Viet binh luan...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Gray,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAttachClick) {
                Icon(
                    painter = painterResource(R.drawable.icon_image),
                    contentDescription = "Attach",
                    tint = Color.Black
                )
            }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = onSend,
                enabled = (state.commentInput.isNotBlank() || state.selectedMediaUris.isNotEmpty()) && !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text("Gui", color = Color.Black)
                }
            }
        }
    }
}
