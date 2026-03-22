package com.example.frontend.presentation.screen.postdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Comment
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.component.PostMediaGallery
import com.example.frontend.ui.component.formatTimeAgo
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onMediaSelected(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.loadPostDetail()
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to totalItems
        }
            .map { (lastVisible, totalItems) ->
                totalItems > 0 && lastVisible >= totalItems - 3
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMoreComments()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bài đăng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            CommentInputBar(
                avatarUrl = "",
                input = uiState.commentInput,
                onInputChange = { viewModel.onCommentInputChange(it) },
                selectedMediaUri = uiState.selectedMediaUri,
                replyingToComment = uiState.replyingToComment,
                isSending = uiState.isSendingComment,
                onPickMedia = { mediaPicker.launch("image/* video/*") },
                onRemoveMedia = { viewModel.removeSelectedMedia() },
                onCancelReply = { viewModel.cancelReply() },
                onSend = { viewModel.submitComment() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Bài đăng gốc ──────────────────────────────────────────────
            item {
                uiState.post?.let { post ->
                    PostDetailHeader(
                        post = post,
                        isLiked = uiState.isLiked,
                        likeCount = uiState.likeCount,
                        commentCount = uiState.commentCount,
                        onLikeClick = { viewModel.toggleLike() }
                    )
                }
            }

            // ── Divider + tiêu đề phần bình luận ──────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.commentCount} bình luận",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Loading skeleton ───────────────────────────────────────────
            if (uiState.isLoadingComments) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                }
            }

            // ── Danh sách comment ──────────────────────────────────────────
            items(uiState.comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    onReply = { viewModel.onReplyToComment(comment) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = if (comment.parentCommentId == null) 68.dp else 86.dp,
                        end = 16.dp
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (uiState.isLoadingMoreComments) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            if (uiState.commentsError && uiState.comments.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tải thêm bình luận thất bại.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.loadMoreComments() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            // ── Empty / Error state ────────────────────────────────────────
            if (!uiState.isLoadingComments && uiState.comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có bình luận nào.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (!uiState.isLoadingComments && uiState.commentsError && uiState.comments.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Không thể tải bình luận.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.retryLoadComments() }) {
                            Text("Tải lại")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Post chi tiết (header + content + actions)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostDetailHeader(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onLikeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Avatar + Tên + Thời gian
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.userAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
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
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTimeAgo(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Nội dung bài đăng (full, không truncate)
        Text(
            text = post.content,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Media (ảnh / video)
        if (
            post.cdnUrl.isNotEmpty() ||
            !post.media.isNullOrEmpty() ||
            !post.mediaIds.isNullOrEmpty() ||
            !post.mediaUrls.isNullOrEmpty() ||
            !post.images.isNullOrEmpty() ||
            !post.videos.isNullOrEmpty()
        ) {
            Spacer(Modifier.height(10.dp))
            PostMediaGallery(post = post)
        }

        Spacer(Modifier.height(8.dp))

        // Actions: Like / Comment / Share
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Like button (có animation)
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Thích",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = likeCount.toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(20.dp))

            Icon(
                painter = painterResource(R.drawable.icon_message),
                contentDescription = "Bình luận",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = commentCount.toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(20.dp))

            Icon(
                painter = painterResource(R.drawable.icon_share),
                contentDescription = "Chia sẻ",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = post.shareCount.toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item comment đơn
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CommentItem(
    comment: Comment,
    onReply: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(comment.likeCount) }

    val isReply = comment.parentCommentId != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 34.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        AsyncImage(
            model = comment.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.icon_user)
        )

        Spacer(Modifier.width(12.dp))

        // Nội dung comment
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTimeAgo(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = comment.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!comment.mediaUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                if (comment.mediaType?.contains("video", ignoreCase = true) == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Đã đính kèm video",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AsyncImage(
                        model = comment.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            ),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.icon_image)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Like + Reply
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (likeCount > 0) {
                    Text(
                        text = likeCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                }
                TextButton(
                    onClick = onReply,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Trả lời",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom bar nhập comment
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CommentInputBar(
    avatarUrl: String,
    input: String,
    selectedMediaUri: android.net.Uri?,
    replyingToComment: Comment?,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onPickMedia: () -> Unit,
    onRemoveMedia: () -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (replyingToComment != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đang trả lời ${replyingToComment.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hủy trả lời",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (selectedMediaUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Đã chọn 1 tệp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRemoveMedia, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa tệp",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar của current user
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user)
                )

                Spacer(Modifier.width(10.dp))

                // TextField
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            "Thêm bình luận...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = onPickMedia, enabled = !isSending) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Đính kèm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Send button
                IconButton(
                    onClick = onSend,
                    enabled = !isSending && (input.isNotBlank() || selectedMediaUri != null)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = OrangePrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (input.isNotBlank() || selectedMediaUri != null) OrangePrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
