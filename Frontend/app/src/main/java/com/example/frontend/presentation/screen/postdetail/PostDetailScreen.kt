package com.example.frontend.presentation.screen.postdetail

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.domain.model.User
import com.example.frontend.ui.component.PostMediaPreview
import com.example.frontend.ui.component.ShareDropdownOption
import com.example.frontend.ui.component.ShareFriendItem
import com.example.frontend.ui.component.SharePostCaptionDialog
import com.example.frontend.ui.component.formatTimeAgo
import com.example.frontend.ui.component.toMediaItems
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    currentUser: User? = null,
    postId: String? = null,
    onBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shareFriendsState by viewModel.shareFriendsState.collectAsState()
    val listState = rememberLazyListState()
    val commentItems = remember(uiState.comments) { buildCommentTree(uiState.comments) }

    var shareTargetPost by remember { mutableStateOf<Post?>(null) }
    val postTargets = remember {
        listOf(ShareDropdownOption(id = "feed", label = "Bảng feed"))
    }
    val privacyOptions = remember {
        listOf(
            ShareDropdownOption(id = "public", label = "Công khai"),
            ShareDropdownOption(id = "friends", label = "Bạn bè"),
            ShareDropdownOption(id = "private", label = "Chỉ mình tôi")
        )
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        viewModel.onMediaSelected(uris)
    }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
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
                avatarUrl = currentUser?.avatarUrl.orEmpty(),
                input = uiState.commentInput,
                onInputChange = { viewModel.onCommentInputChange(it) },
                selectedMediaUris = uiState.selectedMediaUris,
                replyingToComment = uiState.replyingToComment,
                isSending = uiState.isSendingComment,
                onPickMedia = {
                    mediaPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onRemoveMedia = { uri -> viewModel.removeSelectedMedia(uri) },
                onClearSelectedMedia = { viewModel.clearSelectedMedia() },
                onCancelReply = { viewModel.cancelReply() },
                onSend = { viewModel.submitComment() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    uiState.post?.let { post ->
                        PostDetailHeader(
                            post = post,
                            isLiked = uiState.isLiked,
                            likeCount = uiState.likeCount,
                            commentCount = uiState.commentCount,
                            onLikeClick = { viewModel.toggleLike() },
                            onShareClick = {
                                shareTargetPost = post
                                viewModel.loadShareFriends(currentUser?.id.orEmpty())
                            }
                        )
                    }
                }

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

                items(commentItems, key = { it.comment.id }) { item ->
                    val comment = item.comment
                    CommentItem(
                        comment = comment,
                        level = item.level,
                        onReply = { viewModel.onReplyToComment(comment) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = 68.dp + (item.level * 18).dp,
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

            shareTargetPost?.let { post ->
                SharePostCaptionDialog(
                    post = post,
                    currentUserId = currentUser?.id.orEmpty(),
                    currentUserName = currentUser?.displayName ?: "Người dùng",
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    postTargets = postTargets,
                    privacyOptions = privacyOptions,
                    friends = shareFriendsState.friends.map { friend ->
                        ShareFriendItem(
                            id = friend.id,
                            name = friend.displayName,
                            avatarUrl = friend.avatarUrl
                        )
                    },
                    isFriendsLoading = shareFriendsState.isLoading,
                    friendsError = shareFriendsState.error,
                    onRetryLoadFriends = {
                        viewModel.loadShareFriends(currentUser?.id.orEmpty(), forceRefresh = true)
                    },
                    onDismiss = { shareTargetPost = null },
                    onConfirmShare = { shareData ->
                        viewModel.sharePost(shareData)
                        shareTargetPost = null
                    }
                )
            }
        }
    }
}

private data class CommentUiItem(
    val comment: Comment,
    val level: Int
)

private fun buildCommentTree(comments: List<Comment>): List<CommentUiItem> {
    if (comments.isEmpty()) return emptyList()

    val byParent = comments.groupBy { it.parentCommentId }
    val roots = byParent[null].orEmpty().sortedByDescending { it.createdAt }
    val result = mutableListOf<CommentUiItem>()
    val seenIds = mutableSetOf<String>()

    fun dfs(node: Comment, level: Int) {
        if (!seenIds.add(node.id)) return
        result.add(CommentUiItem(node, level))
        val children = byParent[node.id].orEmpty().sortedBy { it.createdAt }
        children.forEach { child -> dfs(child, level + 1) }
    }

    roots.forEach { root -> dfs(root, 0) }

    comments.forEach { orphan ->
        if (!seenIds.contains(orphan.id)) {
            result.add(
                CommentUiItem(
                    comment = orphan,
                    level = if (orphan.parentCommentId == null) 0 else 1
                )
            )
        }
    }

    return result
}

@Composable
private fun PostDetailHeader(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val mediaItems = post.toMediaItems()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
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

        Text(
            text = post.content,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (mediaItems.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .background(Color.Black)
            ) {
                PostMediaPreview(mediaItems = mediaItems)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
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

            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_share),
                    contentDescription = "Chia sẻ",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = post.shareCount.toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    level: Int,
    onReply: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(comment.likeCount) }
    val commentMedia = remember(comment.media, comment.mediaUrl, comment.mediaType) {
        resolveCommentMedia(comment)
    }

    val startPadding = 16.dp + (level * 18).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = startPadding,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.Top
    ) {
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
            if (commentMedia.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PostMediaPreview(mediaItems = commentMedia)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun CommentInputBar(
    avatarUrl: String,
    input: String,
    selectedMediaUris: List<android.net.Uri>,
    replyingToComment: Comment?,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onPickMedia: () -> Unit,
    onRemoveMedia: (android.net.Uri) -> Unit,
    onClearSelectedMedia: () -> Unit,
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

            if (selectedMediaUris.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMediaUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.icon_image)
                                )
                                IconButton(
                                    onClick = { onRemoveMedia(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xoa tep",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onClearSelectedMedia, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xoa tat ca tep",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                IconButton(
                    onClick = onSend,
                    enabled = !isSending && (input.isNotBlank() || selectedMediaUris.isNotEmpty())
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
                            tint = if (input.isNotBlank() || selectedMediaUris.isNotEmpty()) OrangePrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
