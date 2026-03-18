package com.example.frontend.ui.component

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.OriginalPost
import com.example.frontend.domain.model.Post

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onSaveClick: (() -> Unit)? = null,
    saveMenuLabel: String = "Lưu bài viết",
    onShareClick: (() -> Unit)? = null,
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
