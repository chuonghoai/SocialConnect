package com.example.frontend.ui.component

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDateTime
import kotlin.math.hypot

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

    val postMedia = resolveMedia(post.kind, post.cdnUrl, post.media)

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
                                text = { Text(saveMenuLabel) },
                                onClick = {
                                    isMoreMenuExpanded = false
                                    onSaveClick?.invoke()
                                },
                                enabled = onSaveClick != null
                            )
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
                            text = { Text(saveMenuLabel) },
                            enabled = onSaveClick != null,
                            onClick = {
                                isMoreMenuExpanded = false
                                onSaveClick?.invoke()
                            }
                        )
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
                        DropdownMenuItem(
                            text = { Text("Ẩn bài viết") },
                            onClick = {
                                isMoreMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Báo cáo bài viết") },
                            onClick = {
                                isMoreMenuExpanded = false
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

                if (postMedia.isNotEmpty()) {
                    PostMediaGallery(
                        mediaItems = postMedia,
                        isLiked = post.isLiked,
                        likeCount = post.likeCount,
                        commentCount = post.commentCount,
                        shareCount = post.shareCount,
                        onLikeClick = onLikeClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick
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
                    enabled = onShareClick != null,
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

private data class PostMediaItem(
    val url: String,
    val kind: String
)

@Composable
private fun SharedPostPreviewCard(originalPost: OriginalPost) {
    val originalMedia = resolveMedia(originalPost.kind, originalPost.cdnUrl, originalPost.media)
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

            if (originalMedia.isNotEmpty()) {
                PostMediaGallery(mediaItems = originalMedia)
            }
            if (mediaItems.isNotEmpty()) {
                PostMediaPreview(mediaItems = mediaItems)
            }
        }
    }
}

private fun resolveMedia(kind: String, cdnUrl: String, media: List<PostMedia>): List<PostMedia> {
    val normalizedFromList = media
        .filter { it.cdnUrl.isNotBlank() }
        .map {
            if (it.kind.isBlank()) it.copy(kind = kind.ifBlank { "IMAGE" }) else it
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
private fun PostMediaPreview(
    mediaItems: List<PostMediaItem>,
    onVideoClick: (() -> Unit)? = null
) {
    val firstItem = mediaItems.firstOrNull() ?: return
    val isVideo = firstItem.kind.equals("VIDEO", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isVideo && onVideoClick != null) { onVideoClick?.invoke() }
    ) {
        AsyncImage(
            model = firstItem.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.icon_image)
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_play_video),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
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
                    FullscreenVideo(url = item.cdnUrl)
                } else {
                    AsyncImage(
                        model = item.cdnUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.icon_image)
                    )
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
private fun FullscreenVideo(url: String) {
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

private fun Post.toMediaItems(): List<PostMediaItem> {
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
            PostMediaItem(url = url, kind = normalizedKind)
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
        PostMediaItem(url = url, kind = normalizedKind)
    }
}

private fun List<PostMedia>.toPostMediaItems(): List<PostMediaItem> {
    return mapNotNull { mediaItem ->
        val url = mediaItem.resolvedUrl().trim()
        if (url.isBlank()) {
            null
        } else {
            val normalizedKind = normalizeKind(
                rawKind = mediaItem.kind?.ifBlank { null },
                url = url
            )
            PostMediaItem(url = url, kind = normalizedKind)
        }
    }
}

private fun OriginalPost.toMediaItems(): List<PostMediaItem> {
    val urls = parseMediaUrls(cdnUrl)
    if (urls.isEmpty()) return emptyList()

    val kinds = parseKinds(kind)
    return urls.mapIndexed { index, url ->
        val normalizedKind = normalizeKind(
            rawKind = kinds.getOrNull(index) ?: kinds.firstOrNull(),
            url = url
        )
        PostMediaItem(url = url, kind = normalizedKind)
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
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
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

fun formatTimeAgo(timeString: String): String {
    return try {
        timeString.substring(0, 10)
    } catch (_: Exception) {
        "Vừa xong"
    }
}

