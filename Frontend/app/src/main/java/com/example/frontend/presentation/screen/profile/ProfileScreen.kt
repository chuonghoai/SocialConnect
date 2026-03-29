package com.example.frontend.presentation.screen.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.component.SharePostCaptionDialog
import com.example.frontend.ui.component.ShareDropdownOption
import com.example.frontend.ui.component.ShareFriendItem
import com.example.frontend.ui.component.ScrollToTopButton
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onBackClick: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onEditPostClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val successState = uiState as? ProfileUiState.Success
    val currentUser = successState?.user
    val selectedTabIndex = successState?.selectedTabIndex ?: 0
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

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    val shouldLoadMore by remember(selectedTabIndex) {
        derivedStateOf {
            if (selectedTabIndex != 0) {
                false
            } else {
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleItem >= totalItems - 3
            }
        }
    }

    LaunchedEffect(shouldLoadMore, selectedTabIndex) {
        if (shouldLoadMore && selectedTabIndex == 0) viewModel.loadMorePosts()
    }

    LaunchedEffect(Unit) {
        if (uiState is ProfileUiState.Loading) {
            viewModel.load()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ProfileTopBar(onBackClick = onBackClick, onMoreClick = onNavigateToSetting)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                        ProfileInfoSection(
                            user = currentUser,
                            onNavigateToFriends = onNavigateToFriends
                        )
                        ProfileTabs(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = viewModel::onTabSelected
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }

                    when (val state = uiState) {
                        is ProfileUiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = OrangePrimary)
                                }
                            }
                        }

                        is ProfileUiState.Error -> {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 50.dp, bottom = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { viewModel.load(isRefresh = true) }) { Text("Thử lại") }
                                    Spacer(Modifier.height(12.dp))
                                    TextButton(onClick = { viewModel.logout(onLoggedOut) }) { Text("Đăng xuất") }
                                }
                            }
                        }

                        is ProfileUiState.Success -> {
                            val isSavedTab = state.selectedTabIndex == 1
                            val isTabLoading = if (isSavedTab) state.isSavedPostsLoading else state.isPostsLoading
                            val tabError = if (isSavedTab) state.savedPostsError else state.postsError
                            val tabPosts: List<Post> = if (isSavedTab) state.savedPosts else state.posts

                            if (isTabLoading) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = OrangePrimary)
                                    }
                                }
                            } else if (tabError != null) {
                                item {
                                    Text(
                                        text = tabError,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else if (tabPosts.isEmpty()) {
                                item {
                                    Text(
                                        text = if (isSavedTab) "Bạn chưa lưu bài viết nào" else "Bạn chưa có bài viết nào",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(tabPosts) { post ->
                                    PostCard(
                                        post = post,
                                        isOwnPost = currentUser?.id == post.userId,
                                        onSaveClick = { viewModel.toggleSavePost(post.id) },
                                        saveMenuLabel = if (post.isSaved) "Bỏ lưu bài viết" else "Lưu bài viết",
                                        onShareClick = {
                                            shareTargetPost = post
                                            viewModel.loadShareFriends(currentUser?.id.orEmpty())
                                        },
                                        onEditPostRequest = {
                                            onEditPostClick(post.id)
                                        },
                                        onChangeVisibility = { visibility ->
                                            viewModel.changePostVisibility(post.id, visibility)
                                        },
                                        onDeletePost = {
                                            viewModel.deletePost(post.id)
                                        },
                                        onHidePost = {
                                            viewModel.hidePost(post.id)
                                        },
                                        onReportPost = { postId, reason ->
                                            viewModel.reportPost(postId, reason)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (!isSavedTab && state.isLoadingMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = OrangePrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                ScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
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

@Composable
private fun ProfileTopBar(onBackClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Hồ sơ cá nhân",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onMoreClick) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                modifier = Modifier.size(28.dp)
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
private fun ProfileInfoSection(
    user: User?,
    onNavigateToFriends: () -> Unit = {}
) {
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, OrangePrimary, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user)
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = user.postCount.toString(), label = "Bài đăng")
                StatItem(
                    count = user.friendCount.toString(),
                    label = "Bạn bè",
                    onClick = onNavigateToFriends
                )
            }
            Spacer(Modifier.height(16.dp))

            if (!user.caption.isNullOrBlank()) {
                Text(
                    text = user.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(skeletonColor)
            )
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.width(140.dp).height(24.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.width(100.dp).height(16.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(36.dp, 24.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.size(60.dp, 16.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(36.dp, 24.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.size(60.dp, 16.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
                }
            }
            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.width(220.dp).height(14.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.width(160.dp).height(14.dp).background(skeletonColor, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun StatItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        }
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = OrangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = OrangePrimary
            )
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Posts",
                    tint = if (selectedTabIndex == 0) OrangePrimary else Color.Gray
                )
            }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Saved",
                    tint = if (selectedTabIndex == 1) OrangePrimary else Color.Gray
                )
            }
        )
    }
}


