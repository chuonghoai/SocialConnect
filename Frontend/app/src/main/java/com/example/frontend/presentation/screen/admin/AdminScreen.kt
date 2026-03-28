package com.example.frontend.presentation.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.frontend.domain.model.AdminUserItem
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.theme.OrangePrimary

val BackgroundLightGray = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLoggedOut: () -> Unit,
    onUserClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundLightGray,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp), // KHẮC PHỤC DOUBLE PADDING HEADER
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.DarkGray
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = state.currentAdminAvatarUrl,
                            contentDescription = "Admin Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, OrangePrimary, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.icon_user),
                            placeholder = painterResource(R.drawable.icon_user)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentTab() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = OrangePrimary
                        )
                    }
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFE53935)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets(0.dp), // KHẮC PHỤC DOUBLE PADDING BOTTOM BAR
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = state.selectedTabIndex == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PeopleAlt,
                            contentDescription = "Users"
                        )
                    },
                    label = { Text("Người dùng", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTabIndex == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "Posts"
                        )
                    },
                    label = { Text("Bài viết", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        selectedTextColor = OrangePrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLightGray)
        ) {
            state.error?.let {
                BannerText(
                    text = it,
                    background = Color(0xFFFFEBEE),
                    textColor = Color(0xFFD32F2F)
                )
            }

            state.message?.let {
                BannerText(
                    text = it,
                    background = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32)
                )
            }

            when (state.selectedTabIndex) {
                0 -> UsersTabContent(
                    users = state.users,
                    keyword = state.keyword,
                    isLoading = state.isUserLoading,
                    actionLoadingUserId = state.actionLoadingUserId,
                    lockedUserIds = state.lockedUserIds,
                    onKeywordChange = viewModel::onKeywordChange,
                    onSearch = viewModel::searchUsers,
                    onLockUser = viewModel::lockUser,
                    onUnlockUser = viewModel::unlockUser,
                    onDeleteUser = viewModel::deleteUser,
                    onViewProfile = onUserClick
                )

                else -> PostsTabContent(
                    posts = state.posts,
                    hiddenPostIds = state.hiddenPostIds,
                    isLoading = state.isPostLoading,
                    onTogglePostVisibility = viewModel::togglePostVisibility,
                    onDeletePost = viewModel::removePost
                )
            }
        }
    }
}

@Composable
private fun BannerText(
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
private fun UsersTabContent(
    users: List<AdminUserItem>,
    keyword: String,
    isLoading: Boolean,
    actionLoadingUserId: String?,
    lockedUserIds: Set<String>,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLockUser: (String) -> Unit,
    onUnlockUser: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onViewProfile: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Tìm user theo tên/email...", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Button(
                onClick = onSearch,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Tìm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return
        }

        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có user để hiển thị",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserAdminCard(
                    user = user,
                    isLocked = lockedUserIds.contains(user.id),
                    isActionLoading = actionLoadingUserId == user.id,
                    onLockUser = { onLockUser(user.id) },
                    onUnlockUser = { onUnlockUser(user.id) },
                    onDeleteUser = { onDeleteUser(user.id) },
                    onViewProfile = { onViewProfile(user.id, lockedUserIds.contains(user.id)) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun UserAdminCard(
    user: AdminUserItem,
    isLocked: Boolean,
    isActionLoading: Boolean,
    onLockUser: () -> Unit,
    onUnlockUser: () -> Unit,
    onDeleteUser: () -> Unit,
    onViewProfile: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isLocked) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFEEEEEE), CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user),
                    placeholder = painterResource(R.drawable.icon_user)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName.ifBlank { user.username },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.DarkGray
                    )
                }
                if (isLocked) {
                    Text(
                        text = "ĐÃ KHÓA",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (isLocked) onUnlockUser() else onLockUser()
                    },
                    enabled = !isActionLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) Color(0xFF2E7D32) else OrangePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isLocked) "Mở khóa" else "Khóa user", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onDeleteUser,
                    enabled = !isActionLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    Text("Xóa user", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PostsTabContent(
    posts: List<Post>,
    hiddenPostIds: Set<String>,
    isLoading: Boolean,
    onTogglePostVisibility: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Danh sách bài viết hiển thị theo giao diện feed",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        HorizontalDivider(
            color = Color(0xFFEEEEEE),
            thickness = 1.dp
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return
        }

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có bài viết nào để hiển thị",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(posts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    PostCard(
                        post = post,
                        adminMode = true,
                        isHiddenByAdmin = hiddenPostIds.contains(post.id),
                        onHidePost = { onTogglePostVisibility(post.id) },
                        onDeletePost = { onDeletePost(post.id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}