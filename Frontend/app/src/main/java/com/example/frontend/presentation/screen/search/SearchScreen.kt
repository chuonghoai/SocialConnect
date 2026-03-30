package com.example.frontend.presentation.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.SearchResult
import com.example.frontend.domain.model.SearchUserItem
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    currentUserId: String? = null,
    onUserClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var selectedUserForAction by remember { mutableStateOf<SearchUserItem?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = { Text("Tìm kiếm người dùng, bài viết...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.clearQuery()
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = Color.Gray)
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.search()
                })
            )
        }

        if (uiState.query.isNotEmpty() || uiState.hasSearched) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchScope.values().forEach { scope ->
                    FilterChip(
                        selected = uiState.scope == scope,
                        onClick = { viewModel.onScopeChange(scope) },
                        label = { Text(scope.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = OrangePrimary
                    )
                }

                uiState.hasSearched -> {
                    when {
                        uiState.error != null -> {
                            val errorMsg = uiState.error ?: ""
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 50.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.search() }) {
                                    Text("Thử lại")
                                }
                            }
                        }

                        uiState.results != null -> {
                            val results: SearchResult = uiState.results!!
                            val showUsers = uiState.scope == SearchScope.ALL || uiState.scope == SearchScope.USER
                            val showPosts = uiState.scope == SearchScope.ALL || uiState.scope == SearchScope.POST
                            val filteredUsers: List<SearchUserItem> = if (showUsers) {
                                results.users.filter { user -> user.id != currentUserId }
                            } else {
                                emptyList()
                            }
                            val filteredPosts: List<Post> = if (showPosts) results.posts else emptyList()

                            if (filteredUsers.isEmpty() && filteredPosts.isEmpty()) {
                                SearchEmptyResult("Không tìm thấy kết quả nào cho \"${results.keyword}\"")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    if (filteredUsers.isNotEmpty()) {
                                        item { SearchSectionHeader("Người dùng (${filteredUsers.size})") }
                                        items(filteredUsers, key = { it.id }) { user ->
                                            val userStatus = normalizeFriendshipStatus(user.friendshipStatus)
                                            UserSearchItem(
                                                user = user,
                                                isFriendActionLoading = uiState.addingFriendIds.contains(user.id),
                                                hasPendingSentRequest = uiState.pendingSentFriendIds.contains(user.id) ||
                                                    isSentPendingStatus(userStatus),
                                                hasPendingIncomingRequest = uiState.pendingIncomingFriendIds.contains(user.id) ||
                                                    isIncomingPendingStatus(userStatus),
                                                onFriendClick = { btnText ->
                                                    if (btnText == "Kết bạn") {
                                                        viewModel.addFriend(user.id)
                                                    } else {
                                                        selectedUserForAction = user
                                                        showBottomSheet = true
                                                    }
                                                },
                                                onUserClick = { onUserClick(user.id) },
                                                onDeleteClick = { viewModel.deleteFriend(user.id) }
                                            )
                                        }
                                    }
                                    if (filteredPosts.isNotEmpty()) {
                                        item { SearchSectionHeader("Bài viết (${filteredPosts.size})") }
                                        items(filteredPosts, key = { it.id }) { post ->
                                            PostCard(post = post)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.query.isEmpty() -> {
                    SearchHistorySection(
                        history = uiState.searchHistory,
                        onSelect = { viewModel.selectHistory(it) },
                        onDelete = { viewModel.deleteHistory(it) },
                        onClearAll = { viewModel.clearHistory() }
                    )
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nhấn tìm kiếm để xem kết quả",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        if (showBottomSheet && selectedUserForAction != null) {
            val user = selectedUserForAction!!

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
                    val normalizedStatus = normalizeFriendshipStatus(user.friendshipStatus)

                    if (user.isFriend || normalizedStatus == "FRIEND") {
                        ListItem(
                            headlineContent = { Text("Hủy kết bạn", color = Color.Red) },
                            modifier = Modifier.clickable {
                                viewModel.deleteFriend(user.id)
                                showBottomSheet = false
                            }
                        )
                    } else {
                        when (normalizedStatus) {
                            "REQUEST_SENT", "PENDING", "OUTGOING_PENDING" -> {
                                ListItem(
                                    headlineContent = { Text("Thu hồi lời mời") },
                                    modifier = Modifier.clickable {
                                        viewModel.cancelRequest(user.id)
                                        showBottomSheet = false
                                    }
                                )
                            }
                            "REQUEST_RECEIVED", "INCOMING_PENDING" -> {
                                ListItem(
                                    headlineContent = { Text("Chấp nhận") },
                                    modifier = Modifier.clickable {
                                        viewModel.acceptRequest(user.id)
                                        showBottomSheet = false
                                    }
                                )
                                ListItem(
                                    headlineContent = { Text("Từ chối") },
                                    modifier = Modifier.clickable {
                                        viewModel.rejectRequest(user.id)
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
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa có lịch sử tìm kiếm", color = Color.Gray, fontSize = 15.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử tìm kiếm",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onClearAll) {
                    Text("Xóa tất cả", color = OrangePrimary, fontSize = 13.sp)
                }
            }
        }

        items(history, key = { it }) { keyword ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(keyword) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = keyword,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { onDelete(keyword) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xóa khỏi lịch sử",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun UserSearchItem(
    user: SearchUserItem,
    isFriendActionLoading: Boolean = false,
    hasPendingSentRequest: Boolean = false,
    hasPendingIncomingRequest: Boolean = false,
    onFriendClick: (String) -> Unit = {},
    onUserClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val buttonText = when {
        user.isFriend -> "Bạn bè"
        hasPendingIncomingRequest -> "Đã nhận lời mời"
        hasPendingSentRequest -> "Đã gửi lời mời"
        isFriendActionLoading -> "Đang gửi..."
        else -> "Kết bạn"
    }
    val canClick = !isFriendActionLoading

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {onUserClick()}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        Button(
            onClick = { onFriendClick(buttonText) },
            enabled = !isFriendActionLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (buttonText == "Kết bạn") OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (buttonText == "Kết bạn") Color.White else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SearchEmptyResult(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.Gray, fontSize = 15.sp)
    }
}

private fun normalizeFriendshipStatus(raw: String?): String {
    return raw.orEmpty().trim().uppercase()
}

private fun isSentPendingStatus(status: String): Boolean {
    return status in setOf("PENDING", "REQUEST_SENT", "OUTGOING_PENDING")
}

private fun isIncomingPendingStatus(status: String): Boolean {
    return status in setOf("REQUEST_RECEIVED", "INCOMING_PENDING")
}
