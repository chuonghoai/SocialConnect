package com.example.frontend.presentation.screen.otherprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.frontend.ui.component.ScrollToTopButton
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    onPostClick: (Post) -> Unit = {},
    onNavigateToFriends: (String) -> Unit = {},
    viewModel: OtherProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.load(isRefresh = true)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMorePosts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OtherProfileTopBar(onBackClick = onBackClick)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                is OtherProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        color = OrangePrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is OtherProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(isRefresh = true) }) {
                            Text("Thử lại")
                        }
                    }
                }

                is OtherProfileUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            OtherProfileInfoSection(
                                user = state.user,
                                onAccept = { viewModel.acceptRequest() },
                                onReject = { viewModel.rejectRequest() },
                                onFriendButtonClick = {
                                    if (state.user.friendshipStatus == "NONE") {
                                        viewModel.addFriend()
                                    } else {
                                        showBottomSheet = true
                                    }
                                },
                                onNavigateToFriends = onNavigateToFriends
                            )
                            Divider(
                                thickness = 8.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(state.posts) { post ->
                                PostCard(
                                    post = post,
                                    onCommentClick = { onPostClick(post) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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

                        if (!state.error.isNullOrBlank()) {
                            item {
                                Text(
                                    text = state.error,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
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

        if (showBottomSheet && uiState is OtherProfileUiState.Success) {
            val user = (uiState as OtherProfileUiState.Success).user

            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "Tùy chọn với ${user.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider()

                    when (user.friendshipStatus) {
                        "FRIEND" -> {
                            ListItem(
                                headlineContent = { Text("Hủy kết bạn", color = Color.Red) },
                                modifier = Modifier.clickable {
                                    viewModel.unfriend()
                                    showBottomSheet = false
                                }
                            )
                        }
                        "REQUEST_SENT" -> {
                            ListItem(
                                headlineContent = { Text("Thu hồi lời mời") },
                                modifier = Modifier.clickable {
                                    viewModel.cancelRequest()
                                    showBottomSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherProfileTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Trang cá nhân",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
    Divider(thickness = 0.5.dp)
}

@Composable
private fun OtherProfileInfoSection(
    user: User,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onFriendButtonClick: () -> Unit,
    onNavigateToFriends: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                Icons.Default.CheckCircle,
                contentDescription = null,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = user.postCount.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(text = "Bài đăng", color = Color.Gray, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.clickable { onNavigateToFriends(user.id) }) {
                Text(text = user.friendCount.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(text = "Bạn bè", color = Color.Gray, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!user.caption.isNullOrBlank()) {
            Text(
                text = user.caption,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
        }

        if (user.friendshipStatus == "REQUEST_RECEIVED") {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Chấp nhận")
                }
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Từ chối")
                }
            }
        } else {
            val buttonText = when (user.friendshipStatus) {
                "FRIEND" -> "Bạn bè"
                "REQUEST_SENT" -> "Đã gửi lời mời"
                else -> "Kết bạn"
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onFriendButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.friendshipStatus == "NONE") OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (user.friendshipStatus == "NONE") Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
