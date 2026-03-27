package com.example.frontend.presentation.screen.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.AdminUserItem
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLoggedOut: () -> Unit,
    onUserClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = state.currentAdminAvatarUrl,
                            contentDescription = "Admin Avatar",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.icon_user),
                            placeholder = painterResource(R.drawable.icon_user)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentTab() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = NavigationBarDefaults.windowInsets
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
                    label = { Text("Users") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                    label = { Text("Posts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            state.error?.let {
                BannerText(
                    text = it,
                    background = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    textColor = MaterialTheme.colorScheme.error
                )
            }

            state.message?.let {
                BannerText(
                    text = it,
                    background = Color(0xFF1B8F3A).copy(alpha = 0.14f),
                    textColor = Color(0xFF1B8F3A)
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodySmall)
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
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Tim user theo ten/email") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Button(onClick = onSearch) {
                Text("Tim")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
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
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chua co user de hien thi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            item { Spacer(modifier = Modifier.height(12.dp)) }
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
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user),
                    placeholder = painterResource(R.drawable.icon_user)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName.ifBlank { user.username },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isLocked) {
                    Text(
                        text = "LOCKED",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isLocked) onUnlockUser() else onLockUser()
                    },
                    enabled = !isActionLoading
                ) {
                    Text(if (isLocked) "Mo khoa user" else "Khoa user")
                }
                Button(
                    onClick = onDeleteUser,
                    enabled = !isActionLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Xoa user")
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
            text = "Danh sach bai viet hien thi theo giao dien feed client",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
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
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chua co bai viet nao de hien thi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    adminMode = true,
                    isHiddenByAdmin = hiddenPostIds.contains(post.id),
                    onHidePost = { onTogglePostVisibility(post.id) },
                    onDeletePost = { onDeletePost(post.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
