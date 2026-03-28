package com.example.frontend.presentation.screen.home

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User
import com.example.frontend.presentation.viewmodel.WebSocketViewModel
import com.example.frontend.ui.theme.OrangePrimary
import java.time.LocalDateTime
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import com.example.frontend.presentation.navigation.Routes
import com.example.frontend.ui.theme.OrangeLight
import kotlinx.coroutines.launch
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.component.SharePostCaptionDialog
import com.example.frontend.ui.component.ShareDropdownOption
import com.example.frontend.ui.component.ShareFriendItem
import com.example.frontend.ui.component.ScrollToTopButton
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User?,
    onNavigateToMessages: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onEditPostClick: (String) -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    onVideoClick: (postId: String, videoUrl: String) -> Unit = { _, _ -> },
    onAvatarClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    webSocketViewModel: WebSocketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val uploadState by viewModel.uploadState.collectAsState()
    val shareFriendsState by viewModel.shareFriendsState.collectAsState()
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

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    val isConnected by webSocketViewModel.isConnected.collectAsState()
    
    // Chỉ gọi kết nối khi màn hình được khởi tạo và chưa có kết nối
    LaunchedEffect(Unit) {
        if (!isConnected) {
            webSocketViewModel.connect()
        }
    }

    LaunchedEffect(Unit) {
        if (uiState is HomeUiState.Loading) {
            viewModel.load()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(onNavigateToMessages = onNavigateToMessages, onCreatePostClick = onCreatePostClick)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.load(isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        CreatePostSection(
                            user = currentUser ?: User(
                                id = "",
                                email = "",
                                displayName = "Người dùng",
                                avatarUrl = "",
                                username = "",
                                phone = "",
                                role = "USER",
                                isOnline = false,
                                postCount = 0,
                                friendCount = 0,
                                caption = ""
                            ),
                            onCreatePostClick = onCreatePostClick
                        )
                    }

                    if (uploadState.isUploading) {
                        item {
                            Box {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = uploadState.isUploading,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 })
                                ) {
                                    UploadingIndicator(progressText = uploadState.progressText)
                                }
                            }
                        }
                    }

                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = OrangePrimary)
                                }
                            }
                        }

                        is HomeUiState.Error -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 50.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = state.message, color = Color.Red)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { viewModel.load() }) {
                                        Text("Thử lại")
                                    }
                                }
                            }
                        }

                        is HomeUiState.Success -> {
                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    isOwnPost = currentUser?.id == post.userId,
                                    onLikeClick = {
                                        viewModel.toggleLike(post.id)
                                    },
                                    onCommentClick = {
                                        viewModel.selectPost(post)
                                        onPostClick(post)
                                    },
                                    onSaveClick = {
                                        viewModel.savePost(post.id)
                                    },
                                    saveMenuLabel = if (post.isSaved) "Bỏ lưu bài viết" else "Lưu bài viết",
                                    onShareClick = {
                                        shareTargetPost = post
                                        viewModel.loadShareFriends(currentUser?.id.orEmpty(), forceRefresh = true)
                                    },
                                    onAvatarClick = onAvatarClick,
                                    onVideoClick = { clickedVideoUrl ->
                                        onVideoClick(post.id, clickedVideoUrl)
                                    },
                                    onEditPostRequest = {
                                        onEditPostClick(post.id)
                                    },
                                    onDeletePost = {
                                        viewModel.deletePost(post.id)
                                    },
                                    onChangeVisibility = { visibility ->
                                        viewModel.changePostVisibility(post.id, visibility)
                                    },
                                    onHidePost = {
                                        viewModel.hidePost(post.id)
                                    },
                                    onReportPost = {
                                        viewModel.reportPost()
                                    }
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
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
                        viewModel.loadShareFriends(
                            currentUserId = currentUser?.id.orEmpty(),
                            forceRefresh = true
                        )
                    },
                    onDismiss = { shareTargetPost = null },
                    onConfirmShare = { shareData ->
                        Log.d(
                            "SharePost",
                            "shareNow postId=${shareData.postId}, shareText=${shareData.shareText}, " +
                                "target=${shareData.target}, privacy=${shareData.privacy}, " +
                                "selectedFriendIds=${shareData.selectedFriendIds.joinToString()}, " +
                                "currentUserId=${shareData.currentUserId}"
                        )
                        // TODO(BE): hỗ trợ gửi caption + selectedFriendIds trong endpoint share/message để FE nối payload đầy đủ.
                        viewModel.sharePost(shareData)
                        shareTargetPost = null
                    }
                )
            }
        }
    }
}

// Header
@Composable
fun HomeHeader(
    onNavigateToMessages: () -> Unit = {},
    onCreatePostClick: () -> Unit = {}
) {
    Surface(
        shadowElevation = 3.dp,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AliceApp",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                style = MaterialTheme.typography.headlineMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = onCreatePostClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_plus),
                        contentDescription = "New Post",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onNavigateToMessages,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_message),
                        contentDescription = "Messages",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// Create new post
@Composable
fun CreatePostSection(user: User, onCreatePostClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onCreatePostClick() }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Bạn đang nghĩ gì?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_image),
                contentDescription = "Add Image",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onCreatePostClick() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UploadingIndicator(progressText: String) {
    Surface(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Đang đăng, vui lòng không tắt ứng dụng",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (progressText.isNotBlank()) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
