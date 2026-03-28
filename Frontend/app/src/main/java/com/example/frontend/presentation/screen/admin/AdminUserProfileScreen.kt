package com.example.frontend.presentation.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.frontend.ui.component.ScrollToTopButton
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@Composable
fun AdminUserProfileScreen(
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
    onPostClick: (Post) -> Unit = {},
    viewModel: AdminUserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMorePosts()
    }

    // Sử dụng Scaffold để bao bọc và xử lý Double Padding chuẩn nhất
    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = { AdminUserProfileTopBar(onBackClick = onBackClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            state.error?.let {
                AdminBannerText(
                    text = it,
                    background = Color(0xFFFFEBEE),
                    textColor = Color(0xFFD32F2F)
                )
            }

            state.message?.let {
                AdminBannerText(
                    text = it,
                    background = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.isLoading && state.user == null) {
                    CircularProgressIndicator(
                        color = OrangePrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (state.user == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "Không thể tải trang cá nhân",
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.load(isRefresh = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Thử lại", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            AdminUserInfoSection(
                                user = state.user,
                                isLocked = state.isLocked,
                                isActionLoading = state.isActionLoading,
                                onToggleLock = viewModel::toggleUserLock,
                                onDeleteUser = { viewModel.deleteUser(onDeleted) }
                            )
                            Divider(
                                thickness = 8.dp,
                                color = Color(0xFFEEEEEE)
                            )
                        }

                        if (state.isPostsLoading) {
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
                        } else if (state.posts.isEmpty()) {
                            item {
                                Text(
                                    text = "Người dùng này chưa có bài viết nào",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            items(state.posts) { post ->
                                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                    PostCard(
                                        post = post,
                                        adminMode = true,
                                        isHiddenByAdmin = state.hiddenPostIds.contains(post.id),
                                        onHidePost = { viewModel.togglePostVisibility(post.id) },
                                        onDeletePost = { viewModel.removePost(post.id) },
                                        onCommentClick = { onPostClick(post) }
                                    )
                                }
                            }
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

                        if (!state.postError.isNullOrBlank()) {
                            item {
                                Text(
                                    text = state.postError ?: "",
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                ScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUserProfileTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        windowInsets = WindowInsets(0.dp), // CHẶN DOUBLE PADDING TOP
        title = {
            Text(
                text = "Chi tiết user",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E1E1E)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E1E1E)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        ),
        modifier = Modifier.shadow(1.dp) // Thêm tí viền đổ bóng nhẹ để tách biệt với nền xám
    )
}

@Composable
private fun AdminBannerText(
    text: String,
    background: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AdminUserInfoSection(
    user: User?,
    isLocked: Boolean,
    isActionLoading: Boolean,
    onToggleLock: () -> Unit,
    onDeleteUser: () -> Unit
) {
    if (user == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(3.dp, OrangePrimary, CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.icon_user)
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1E1E1E)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = user.postCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1E1E1E))
                Text(text = "Bài đăng", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = user.friendCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1E1E1E))
                Text(text = "Bạn bè", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(20.dp))

        if (!user.caption.isNullOrBlank()) {
            Text(
                text = user.caption,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleLock,
                enabled = !isActionLoading,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color(0xFF2E7D32) else OrangePrimary,
                    contentColor = Color.White
                )
            ) {
                Text(if (isLocked) "Mở khóa user" else "Khóa user", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDeleteUser,
                enabled = !isActionLoading,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Text("Xóa user", fontWeight = FontWeight.Bold)
            }
        }
    }
}