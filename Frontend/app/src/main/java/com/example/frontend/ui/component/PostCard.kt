package com.example.frontend.ui.component

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.OriginalPost
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.PostMedia
import kotlin.math.hypot
import java.time.LocalDateTime

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onVideoClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    saveMenuLabel: String = "Lưu bài viết",
    onShareClick: (() -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
    ) {
    var isMoreMenuExpanded by remember { mutableStateOf(false) }

    val isSharedPost = post.type.equals("SHARED", ignoreCase = true)
    val originalPost = post.originalPost
    val shouldRenderOriginalPost = isSharedPost && originalPost != null
    val trimmedContent = post.content.trim()
    val trimmedOriginalContent = originalPost?.content?.trim().orEmpty()
    val shouldShowSharedCaption = shouldRenderOriginalPost &&
        trimmedContent.isNotEmpty() &&
        trimmedContent != trimmedOriginalContent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        DropdownMenuItem(
                            text = { Text(saveMenuLabel) },
                            onClick = {
                                isMoreMenuExpanded = false
                                onSaveClick?.invoke()
                            },
                            enabled = onSaveClick != null
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

                if (post.cdnUrl.isNotEmpty()) {
                    PostMediaContent(kind = post.kind, cdnUrl = post.cdnUrl)
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

            }
        }
    }
}

private data class PostMediaItem(
    val url: String,
    val kind: String
)

@Composable
private fun SharedPostPreviewCard(originalPost: OriginalPost) {
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

            if (originalPost.cdnUrl.isNotEmpty()) {
                PostMediaContent(kind = originalPost.kind, cdnUrl = originalPost.cdnUrl)
            }
        }
    }
}

@Composable
fun PostMediaContent(kind: String, cdnUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        if (kind == "IMAGE") {
            AsyncImage(
                model = cdnUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        } else if (kind == "VIDEO") {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(cdnUrl))
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setOnPreparedListener {
                            it.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
    }
}

@Composable
private fun MediaGridPreview(
    mediaItems: List<PostMediaItem>,
    onItemClick: (Int) -> Unit
) {
    val previewItems = mediaItems.take(4)
    val spacing = 2.dp

    when (previewItems.size) {
        1 -> {
            MediaTile(
                item = previewItems[0],
                modifier = Modifier.fillMaxSize(),
                onClick = { onItemClick(0) }
            )
        }

        2 -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                MediaTile(
                    item = previewItems[0],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onItemClick(0) }
                )
                MediaTile(
                    item = previewItems[1],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onItemClick(1) }
                )
            }
        }

        3 -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                MediaTile(
                    item = previewItems[0],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onItemClick(0) }
                )

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    MediaTile(
                        item = previewItems[1],
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onClick = { onItemClick(1) }
                    )
                    MediaTile(
                        item = previewItems[2],
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onClick = { onItemClick(2) }
                    )
                }
            }
        }

        4 -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    MediaTile(
                        item = previewItems[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onItemClick(0) }
                    )
                    MediaTile(
                        item = previewItems[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onItemClick(1) }
                    )
                }

                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    MediaTile(
                        item = previewItems[2],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onItemClick(2) }
                    )
                    MediaTile(
                        item = previewItems[3],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onItemClick(3) }
                    )
                }
            }
        }

        else -> {
            val topRow = mediaItems.take(2)
            val bottomRow = mediaItems.drop(2).take(3)

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    MediaTile(
                        item = topRow[0],
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        onClick = { onItemClick(0) }
                    )
                    MediaTile(
                        item = topRow[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onItemClick(1) }
                    )
                }

                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    bottomRow.forEachIndexed { localIndex, item ->
                        val globalIndex = localIndex + 2
                        val showOverlay = localIndex == bottomRow.lastIndex && mediaItems.size > 5
                        MediaTile(
                            item = item,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onItemClick(globalIndex) },
                            overlayText = if (showOverlay) "+${mediaItems.size - 5}" else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    item: PostMediaItem,
    modifier: Modifier,
    onClick: () -> Unit,
    overlayText: String? = null
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        if (item.kind == "VIDEO") {
            FeedVideoPlayer(
                videoUrl = item.url,
                shouldPlay = true,
                mediaAspectRatio = 1f,
                onVideoClick = null
            )
        } else {
            AsyncImage(
                model = item.url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (!overlayText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MediaViewerDialog(
    mediaItems: List<PostMediaItem>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
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
                        videoUrl = item.url,
                        shouldPlay = pagerState.currentPage == page,
                        mediaAspectRatio = 16f / 9f,
                        onVideoClick = null
                    )
                } else {
                    ZoomableMediaImage(imageUrl = item.url)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close viewer",
                        tint = Color.White
                    )
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
private fun ZoomableMediaImage(imageUrl: String) {
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
                    }
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
                                val pan = if (lastCentroid != null) centroid - lastCentroid!! else Offset.Zero

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

@Composable
private fun FeedVideoPlayer(
    videoUrl: String,
    shouldPlay: Boolean,
    mediaAspectRatio: Float,
    onVideoClick: (() -> Unit)?
) {
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }
    var hasEnded by remember(videoUrl) { mutableStateOf(false) }
    var videoViewRef by remember(videoUrl) { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(shouldPlay, isPrepared, hasEnded) {
        val videoView = videoViewRef ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect

        if (shouldPlay && !hasEnded && !videoView.isPlaying) {
            videoView.start()
        }

        if (!shouldPlay && videoView.isPlaying) {
            videoView.pause()
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(mediaAspectRatio)
            .clickable(enabled = onVideoClick != null) {
                onVideoClick?.invoke()
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setVideoURI(Uri.parse(videoUrl))
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = false
                        isPrepared = true
                        hasEnded = false
                        if (shouldPlay) start()
                    }
                    setOnCompletionListener {
                        hasEnded = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (hasEnded) {
            FilledTonalButton(
                onClick = {
                    hasEnded = false
                    videoViewRef?.seekTo(0)
                    videoViewRef?.start()
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Xem lại")
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
fun InteractionItem(iconRes: Int, count: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
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
        timeString.substring(0, 10)
    } catch (e: Exception) {
        "Vừa xong"
    }
}

