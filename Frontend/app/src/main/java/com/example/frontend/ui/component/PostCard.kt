package com.example.frontend.ui.component

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.frontend.R
import com.example.frontend.domain.model.OriginalPost
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.ui.theme.OrangePrimary
import kotlin.math.hypot
import java.time.LocalDateTime
import kotlinx.coroutines.delay

@Composable
fun PostCard(
    post: Post,
    isOwnPost: Boolean = false,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onVideoClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    saveMenuLabel: String = "Lưu bài viết",
    onShareClick: (() -> Unit)? = null,
    onEditPostRequest: (() -> Unit)? = null,
    onDeletePost: (() -> Unit)? = null,
    onChangeVisibility: ((String) -> Unit)? = null,
    onHidePost: (() -> Unit)? = null,
    onReportPost: (() -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
) {
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var showVisibilityDialog by remember { mutableStateOf(false) }

    val isSharedPost = post.type.equals("SHARED", ignoreCase = true)
    val originalPost = post.originalPost
    val shouldRenderOriginalPost = isSharedPost && originalPost != null
    val trimmedContent = post.content.trim()
    val trimmedOriginalContent = originalPost?.content?.trim().orEmpty()
    val shouldShowSharedCaption = shouldRenderOriginalPost &&
            trimmedContent.isNotEmpty() &&
            trimmedContent != trimmedOriginalContent
    val mediaItems = post.toMediaItems()
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.userAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(enabled = onAvatarClick != null) { onAvatarClick?.invoke(post.userId) },
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.displayName,
                        style = MaterialTheme.typography.titleMedium,
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

                Box {
                    IconButton(onClick = { isMoreMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = isMoreMenuExpanded,
                        onDismissRequest = { isMoreMenuExpanded = false }
                    ) {
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Sửa bài viết") },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onEditPostRequest?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Đổi quyền bài viết") },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    showVisibilityDialog = true
                                },
                                enabled = onChangeVisibility != null
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bài viết") },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onDeletePost?.invoke()
                                },
                                enabled = onDeletePost != null
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Ẩn bài viết") },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onHidePost?.invoke()
                                },
                                enabled = onHidePost != null
                            )
                            DropdownMenuItem(
                                text = { Text("Báo cáo bài viết") },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onReportPost?.invoke()
                                },
                                enabled = onReportPost != null
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Chia sẻ bài viết") },
                            enabled = onShareClick != null,
                            onClick = {
                                isMoreMenuExpanded = false
                                onShareClick?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sao chép liên kết") },
                            onClick = {
                                isMoreMenuExpanded = false
                                clipboardManager.setText(AnnotatedString("https://socialconnect.app/posts/${post.id}"))
                            }
                        )
                    }
                }
            }

            if (shouldRenderOriginalPost) {
                if (shouldShowSharedCaption) {
                    Text(
                        text = post.content,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                SharedPostPreviewCard(originalPost = originalPost)
            } else {
                if (post.content.isNotBlank()) {
                    Text(
                        text = post.content,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (mediaItems.isNotEmpty()) {
                    PostMediaPreview(
                        mediaItems = mediaItems,
                        onVideoClick = onVideoClick
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LikeComponent(
                    isLiked = post.isLiked,
                    likeCount = post.likeCount,
                    onLikeClick = onLikeClick,
                    isVertical = false
                )
                Spacer(Modifier.width(24.dp))
                InteractionItem(
                    R.drawable.icon_message,
                    post.commentCount.toString(),
                    onClick = onCommentClick
                )
                Spacer(Modifier.width(24.dp))
                InteractionItem(
                    R.drawable.icon_share,
                    post.shareCount.toString(),
                    onClick = { onShareClick?.invoke() }
                )
                Spacer(Modifier.width(24.dp))
                SaveInteractionItem(
                    isSaved = post.isSaved,
                    onClick = onSaveClick
                )
            }
        }
    }

    if (showVisibilityDialog) {
        AlertDialog(
            onDismissRequest = { showVisibilityDialog = false },
            title = { Text("Đổi quyền bài viết") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Công khai", "Bạn bè", "Riêng tư").forEach { option ->
                        Text(
                            text = option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showVisibilityDialog = false
                                    onChangeVisibility?.invoke(option)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVisibilityDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}



@Composable
private fun SharedPostPreviewCard(originalPost: OriginalPost) {
    val mediaItems = originalPost.toMediaItems()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = originalPost.userAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = originalPost.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTimeAgo(originalPost.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (originalPost.content.isNotBlank()) {
                Text(
                    text = originalPost.content,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            if (mediaItems.isNotEmpty()) {
                PostMediaPreview(mediaItems = mediaItems)
            }
        }
    }
}

private fun resolveMedia(kind: String, cdnUrl: String, media: List<PostMedia>?): List<PostMedia> {
    val normalizedFromList = media.orEmpty()
        .filter { it.cdnUrl?.isNotBlank() == true }
        .map {
            if (it.kind?.isBlank() == true) it.copy(kind = kind.ifBlank { "IMAGE" }) else it
        }

    if (normalizedFromList.isNotEmpty()) return normalizedFromList

    return if (cdnUrl.isNotBlank()) {
        listOf(PostMedia(kind = kind.ifBlank { "IMAGE" }, cdnUrl = cdnUrl))
    } else {
        emptyList()
    }
}

@Composable
fun PostMediaContent(kind: String, cdnUrl: String) {
    val mediaItems = resolveMedia(kind = kind, cdnUrl = cdnUrl, media = emptyList())
    if (mediaItems.isNotEmpty()) {
        PostMediaGallery(mediaItems = mediaItems)
    }
}

@Composable
private fun PostMediaGallery(
    mediaItems: List<PostMedia>,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    commentCount: Int = 0,
    shareCount: Int = 0,
    onLikeClick: (() -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null
) {
    var viewerStartIndex by remember(mediaItems) { mutableIntStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        when (mediaItems.size) {
            1 -> {
                MediaTile(
                    item = mediaItems[0],
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    onClick = { viewerStartIndex = 0 }
                )
            }

            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    MediaTile(
                        item = mediaItems[0],
                        modifier = Modifier.weight(1f),
                        onClick = { viewerStartIndex = 0 }
                    )
                    Spacer(Modifier.width(2.dp))
                    MediaTile(
                        item = mediaItems[1],
                        modifier = Modifier.weight(1f),
                        onClick = { viewerStartIndex = 1 }
                    )
                }
            }

            3 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    MediaTile(
                        item = mediaItems[0],
                        modifier = Modifier.weight(1f),
                        onClick = { viewerStartIndex = 0 }
                    )
                    Spacer(Modifier.width(2.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MediaTile(
                            item = mediaItems[1],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 1 }
                        )
                        MediaTile(
                            item = mediaItems[2],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 2 }
                        )
                    }
                }
            }

            else -> {
                val extraCount = mediaItems.size - 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MediaTile(
                            item = mediaItems[0],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 0 }
                        )
                        MediaTile(
                            item = mediaItems[1],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 1 }
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MediaTile(
                            item = mediaItems[2],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 2 }
                        )
                        MediaTile(
                            item = mediaItems[3],
                            modifier = Modifier.weight(1f),
                            onClick = { viewerStartIndex = 3 },
                            overlayText = if (extraCount > 0) "+$extraCount" else null
                        )
                    }
                }
            }
        }
    }

    if (viewerStartIndex >= 0) {
        FullScreenMediaViewer(
            mediaItems = mediaItems,
            initialPage = viewerStartIndex,
            onDismiss = { viewerStartIndex = -1 },
            isLiked = isLiked,
            likeCount = likeCount,
            commentCount = commentCount,
            shareCount = shareCount,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick
        )
    }
}

@Composable
private fun MediaTile(
    item: PostMedia,
    modifier: Modifier,
    onClick: () -> Unit,
    overlayText: String? = null
) {
    val isVideo = item.kind.equals("VIDEO", ignoreCase = true)
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
    ) {
        val model = if (isVideo) {
            ImageRequest.Builder(context)
                .data(item.cdnUrl)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build()
        } else {
            item.cdnUrl
        }

        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.icon_image)
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_play_video),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!overlayText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FullScreenMediaViewer(
    mediaItems: List<PostMedia>,
    initialPage: Int,
    onDismiss: () -> Unit,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    commentCount: Int = 0,
    shareCount: Int = 0,
    onLikeClick: (() -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { mediaItems.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaItems[page]
                if (item.kind.equals("VIDEO", ignoreCase = true)) {
                    FeedVideoPlayer(
                        videoUrl = item.cdnUrl,
                        shouldPlay = pagerState.currentPage == page,
                        mediaAspectRatio = 0f,
                        onVideoClick = null
                    )
                } else {
                    ZoomableMediaImage(imageUrl = item.cdnUrl)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (onLikeClick != null || onCommentClick != null || onShareClick != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onLikeClick != null) {
                            LikeComponent(
                                isLiked = isLiked,
                                likeCount = likeCount,
                                onLikeClick = onLikeClick,
                                isVertical = false,
                                unlikedTint = Color.White.copy(alpha = 0.85f),
                                textColor = Color.White
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        InteractionItem(
                            iconRes = R.drawable.icon_message,
                            count = commentCount.toString(),
                            onClick = {
                                onDismiss()
                                onCommentClick?.invoke()
                            },
                            iconTint = Color.White,
                            textColor = Color.White
                        )

                        Spacer(Modifier.width(24.dp))

                        InteractionItem(
                            iconRes = R.drawable.icon_share,
                            count = shareCount.toString(),
                            onClick = { onShareClick?.invoke() },
                            iconTint = Color.White,
                            textColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenVideo(url: String?) {
    var showReplay by remember(url) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setVideoURI(Uri.parse(url))
                    setOnPreparedListener {
                        showReplay = false
                        start()
                    }
                    setOnCompletionListener {
                        showReplay = true
                    }
                }
            },
            update = {
                it.setVideoURI(Uri.parse(url))
                it.setOnPreparedListener { mp ->
                    showReplay = false
                    mp.isLooping = false
                    it.start()
                }
                it.setOnCompletionListener {
                    showReplay = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center)
        )

        if (showReplay) {
            Button(
                onClick = {
                    showReplay = false
                    videoViewRef?.seekTo(0)
                    videoViewRef?.start()
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Xem lại", textAlign = TextAlign.Center)
            }
        }
    }
}

fun Post.toMediaItems(): List<PostMedia> {
    val mediaFromArray = media.orEmpty().toPostMediaItems()
    if (mediaFromArray.isNotEmpty()) return mediaFromArray

    val mediaFromMediaIds = mediaIds.orEmpty().toPostMediaItems()
    if (mediaFromMediaIds.isNotEmpty()) return mediaFromMediaIds

    val urlsFromArrays = buildList {
        addAll(mediaUrls.orEmpty())
        addAll(images.orEmpty())
        addAll(videos.orEmpty())
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    if (urlsFromArrays.isNotEmpty()) {
        return urlsFromArrays.map { url ->
            val forceKind = if (videos.orEmpty().contains(url)) "VIDEO" else null
            val normalizedKind = normalizeKind(forceKind, url)
            PostMedia(cdnUrl = url, kind = normalizedKind)
        }
    }

    val urls = parseMediaUrls(cdnUrl)
    if (urls.isEmpty()) return emptyList()

    val kinds = parseKinds(kind)
    return urls.mapIndexed { index, url ->
        val normalizedKind = normalizeKind(
            rawKind = kinds.getOrNull(index) ?: kinds.firstOrNull(),
            url = url
        )
        PostMedia(cdnUrl = url, kind = normalizedKind)
    }
}

private fun List<PostMedia>.toPostMediaItems(): List<PostMedia> {
    return mapNotNull { mediaItem ->
        val url = mediaItem.resolvedUrl().trim()
        if (url.isBlank()) {
            null
        } else {
            val normalizedKind = normalizeKind(
                rawKind = mediaItem.kind?.ifBlank { null },
                url = url
            )
            PostMedia(cdnUrl = url, kind = normalizedKind)
        }
    }
}

private fun OriginalPost.toMediaItems(): List<PostMedia> {
    val mediaFromArray = media.orEmpty().toPostMediaItems()
    if (mediaFromArray.isNotEmpty()) return mediaFromArray

    val urls = parseMediaUrls(cdnUrl)
    if (urls.isEmpty()) return emptyList()

    val kinds = parseKinds(kind)
    return urls.mapIndexed { index, url ->
        val normalizedKind = normalizeKind(
            rawKind = kinds.getOrNull(index) ?: kinds.firstOrNull(),
            url = url
        )
        PostMedia(cdnUrl = url, kind = normalizedKind)
    }
}

private fun parseMediaUrls(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return emptyList()

    val extractedByRegex = URL_REGEX.findAll(trimmed)
        .map { it.value.trim() }
        .distinct()
        .toList()

    if (extractedByRegex.size > 1) return extractedByRegex

    val splitCandidates = trimmed.split(Regex("[|,;\\n]"))
        .map { it.cleanToken() }
        .filter { it.isNotEmpty() }

    if (splitCandidates.size > 1) {
        return splitCandidates
    }

    return listOf(trimmed.cleanToken())
}

private fun parseKinds(rawKind: String): List<String> {
    val cleaned = rawKind.trim()
    if (cleaned.isEmpty()) return emptyList()

    return cleaned.split(Regex("[|,;\\n]"))
        .map { it.cleanToken().uppercase() }
        .filter { it.isNotEmpty() }
}

private fun normalizeKind(rawKind: String?, url: String): String {
    if (rawKind == null) return inferKindFromUrl(url)
    if (rawKind.contains("VIDEO")) return "VIDEO"
    if (rawKind.contains("IMAGE")) return "IMAGE"
    return inferKindFromUrl(url)
}

private fun inferKindFromUrl(url: String): String {
    val normalized = url.lowercase()
    if (normalized.contains("/video/") || normalized.contains("resource_type=video")) {
        return "VIDEO"
    }

    val extension = normalized.substringBefore("?").substringAfterLast('.', missingDelimiterValue = "")
    return if (VIDEO_EXTENSIONS.contains(extension)) "VIDEO" else "IMAGE"
}

private fun String.cleanToken(): String {
    return trim()
        .trim('"')
        .trim('\'')
        .trimStart('[')
        .trimEnd(']')
}

private val URL_REGEX = Regex("""https?://[^\s"'|,\]\[]+""")
private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "m3u8", "mkv", "avi", "3gp", "flv")

@Composable
fun InteractionItem(
    iconRes: Int,
    count: String,
    onClick: () -> Unit = {},
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

@Composable
private fun SaveInteractionItem(
    isSaved: Boolean,
    onClick: (() -> Unit)?
) {
    val enabled = onClick != null
    val iconTint = if (isSaved) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Lưu bài viết",
            modifier = Modifier.size(22.dp),
            tint = iconTint
        )
    }
}

@Composable
fun PostMediaPreview(
    mediaItems: List<PostMedia>,
    onVideoClick: (() -> Unit)? = null
) {
    if (mediaItems.isEmpty()) return

    var isViewerOpen by remember(mediaItems) { mutableStateOf(false) }
    var selectedIndex by remember(mediaItems) { mutableStateOf(0) }

    val openViewer: (Int) -> Unit = { index ->
        selectedIndex = index.coerceIn(0, mediaItems.lastIndex)
        isViewerOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        if (mediaItems.size == 1) {
            val item = mediaItems.first()
            if (item.kind == "VIDEO") {
                FeedVideoPlayer(
                    videoUrl = item.cdnUrl,
                    shouldPlay = true,
                    mediaAspectRatio = 16f / 9f,
                    onVideoClick = {
                        if (onVideoClick != null) onVideoClick()
                        else openViewer(0)
                    }
                )
            } else {
                AsyncImage(
                    model = item.cdnUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clickable { openViewer(0) },
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                MediaGridPreview(
                    mediaItems = mediaItems,
                    onItemClick = { index ->
                        val item = mediaItems[index]
                        if (item.kind == "VIDEO" && onVideoClick != null) {
                            onVideoClick()
                        } else {
                            openViewer(index)
                        }
                    }
                )
            }
        }
    }

    if (isViewerOpen) {
        MediaViewerDialog(
            mediaItems = mediaItems,
            initialPage = selectedIndex,
            onDismiss = { isViewerOpen = false }
        )
    }
}

@Composable
private fun MediaGridPreview(
    mediaItems: List<PostMedia>,
    onItemClick: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return

    val previewItems = mediaItems.take(4)
    val hiddenCount = (mediaItems.size - 4).coerceAtLeast(0)
    val spacing = 2.dp

    when (mediaItems.size) {
        1 -> {
            MediaTile(
                item = previewItems[0],
                modifier = Modifier.fillMaxSize(),
                onClick = { onItemClick(0) }
            )
        }

        2 -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                MediaTile(
                    item = previewItems[0],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { onItemClick(0) }
                )
                MediaTile(
                    item = previewItems[1],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { onItemClick(1) }
                )
            }
        }

        3 -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                MediaTile(
                    item = previewItems[0],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { onItemClick(0) }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing)


                ) {
                    MediaTile(
                        item = previewItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onClick = { onItemClick(1) }
                    )

                    MediaTile(
                        item = previewItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onClick = { onItemClick(2) }
                    )
                }
            }
        }

        4 -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    MediaTile(
                        item = previewItems[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(0) }
                    )
                    MediaTile(
                        item = previewItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(1) }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    MediaTile(
                        item = previewItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(2) }
                    )
                    MediaTile(
                        item = previewItems[3],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(3) }
                    )


                }
            }
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    MediaTile(
                        item = previewItems[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(0) }
                    )
                    MediaTile(
                        item = previewItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(1) }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    MediaTile(
                        item = previewItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(2) }
                    )
                    MediaTile(
                        item = previewItems[3],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onItemClick(3) },
                        overlayText = if (hiddenCount > 0) "+$hiddenCount" else null
                    )


                }
            }
        }
    }

}
@Composable
fun FeedVideoPlayer(
    videoUrl: String?,
    shouldPlay: Boolean,
    mediaAspectRatio: Float,
    onVideoClick: (() -> Unit)?,
    onLongPress: (() -> Unit)? = null
) {
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }
    var hasEnded by remember(videoUrl) { mutableStateOf(false) }
    var videoViewRef by remember(videoUrl) { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember(videoUrl) { mutableStateOf<android.media.MediaPlayer?>(null) }

    var showControls by remember { mutableStateOf(false) }
    var isPlaying by remember(shouldPlay) { mutableStateOf(shouldPlay) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var videoDuration by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }

    LaunchedEffect(shouldPlay, isPrepared, hasEnded) {
        val videoView = videoViewRef ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        if (shouldPlay && !hasEnded) {
            videoView.start()
            isPlaying = true
        } else if (!shouldPlay) {
            videoView.pause()
            isPlaying = false
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(isPlaying, isPrepared, showControls) {
        while (isPlaying && isPrepared && showControls) {
            videoViewRef?.let {
                currentPosition = it.currentPosition
            }
            delay(250)
        }
    }

    val baseModifier = if (mediaAspectRatio > 0f) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(mediaAspectRatio)
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = baseModifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        onVideoClick?.invoke()
                    },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setVideoURI(android.net.Uri.parse(videoUrl))
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayerRef = mediaPlayer
                        mediaPlayer.isLooping = false
                        videoDuration = mediaPlayer.duration
                        isPrepared = true
                        hasEnded = false
                        if (shouldPlay) {
                            start()
                            isPlaying = true
                        }
                        mediaPlayer.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                    }
                    setOnCompletionListener {
                        hasEnded = true
                        isPlaying = false
                        showControls = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPrepared && showControls && !hasEnded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newPos = (currentPosition - 5000).coerceAtLeast(0)
                        videoViewRef?.seekTo(newPos)
                        currentPosition = newPos
                    }) {
                        Icon(
                            imageVector = Icons.Default.Replay5,
                            contentDescription = "Lùi 5 giây",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                videoViewRef?.pause()
                                isPlaying = false
                            } else {
                                videoViewRef?.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Phát/Tạm dừng",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    IconButton(onClick = {
                        val newPos = (currentPosition + 5000).coerceAtMost(videoDuration)
                        videoViewRef?.seekTo(newPos)
                        currentPosition = newPos
                    }) {
                        Icon(
                            imageVector = Icons.Default.Forward5,
                            contentDescription = "Tới 5 giây",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatVideoDuration(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = if (videoDuration > 0) currentPosition.toFloat() / videoDuration else 0f,
                        onValueChange = { percent ->
                            val newPos = (percent * videoDuration).toInt()
                            currentPosition = newPos
                            videoViewRef?.seekTo(newPos)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                        )
                    )

                    Text(
                        text = formatVideoDuration(videoDuration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            mediaPlayerRef?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Âm lượng",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (hasEnded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            hasEnded = false
                            videoViewRef?.seekTo(0)
                            videoViewRef?.start()
                            isPlaying = true
                            showControls = false
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Xem lại",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text("Xem lại", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerDialog(
    mediaItems: List<PostMedia>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialPage.coerceIn(0, mediaItems.lastIndex),
                pageCount = { mediaItems.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaItems[page]
                if (item.kind == "VIDEO") {
                    FeedVideoPlayer(
                        videoUrl = item.cdnUrl,
                        shouldPlay = pagerState.currentPage == page,
                        mediaAspectRatio = 0f,
                        onVideoClick = null,
                        onLongPress = { showOptions = true }
                    )
                } else {
                    ZoomableMediaImage(imageUrl = item.cdnUrl, onLongPress = { showOptions = true })
                }
            }

            // Options Overlay
            if (showOptions) {
                val currentMediaItem = mediaItems[pagerState.currentPage]
                MediaOptionBottomSheet(
                    mediaUrl = currentMediaItem.cdnUrl,
                    isVideo = currentMediaItem.kind == "VIDEO",
                    sheetState = sheetState,
                    coroutineScope = coroutineScope,
                    onDismissRequest = {
                        // Tắt modal khi người dùng bấm ra ngoài hoặc vuốt xuống
                        showOptions = false
                    },
                    onShareClick = {
                        // Xử lý share
                    }
                )
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close viewer",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                    color = Color.White,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ZoomableMediaImage(imageUrl: String?, onLongPress: () -> Unit = {}) {
    var scale by remember(imageUrl) { mutableStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(imageUrl) { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                    },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(imageUrl) {
                awaitEachGesture {
                    var lastDistance = 0f
                    var lastCentroid: Offset? = null

                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.filter { it.pressed }

                        if (pressedPointers.size >= 2) {
                            val p1 = pressedPointers[0].position
                            val p2 = pressedPointers[1].position

                            val centroid = Offset(
                                x = (p1.x + p2.x) / 2f,
                                y = (p1.y + p2.y) / 2f
                            )
                            val distance = distanceBetween(p1, p2)

                            if (lastDistance > 0f) {
                                val zoomChange = distance / lastDistance
                                val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                val pan =
                                    if (lastCentroid != null) centroid - lastCentroid!! else Offset.Zero

                                scale = newScale
                                offset = if (newScale > 1f) {
                                    clampOffset(offset + pan, newScale, containerSize)
                                } else {
                                    Offset.Zero
                                }

                                event.changes.forEach { it.consume() }
                            }

                            lastDistance = distance
                            lastCentroid = centroid
                        } else {
                            lastDistance = 0f
                            lastCentroid = null
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(scale, imageUrl) {
                if (scale <= 1f) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = clampOffset(offset + dragAmount, scale, containerSize)
                }
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}

private fun clampOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (scale <= 1f || size == IntSize.Zero) return Offset.Zero

    val maxX = (size.width * (scale - 1f)) / 2f
    val maxY = (size.height * (scale - 1f)) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

private fun distanceBetween(a: Offset, b: Offset): Float {
    return hypot(a.x - b.x, a.y - b.y)
}


fun formatTimeAgo(timeString: String): String {
    return try {
        timeString.substring(0, 10)
    } catch (_: Exception) {
        "Vừa xong"
    }
}

// Helper: format video duration
@SuppressLint("DefaultLocale")
private fun formatVideoDuration(durationMillis: Int): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}