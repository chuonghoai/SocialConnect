package com.example.frontend.presentation.screen.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.model.LastMessage
import com.example.frontend.domain.model.Participant
import com.example.frontend.presentation.viewmodel.SessionViewModel
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary
import com.example.frontend.presentation.viewmodel.WebSocketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onBackClick: () -> Unit = {},
    onNavigateToChat: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: ConversationViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    webSocketViewModel: WebSocketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by sessionViewModel.currentUser.collectAsState()
    val onlineUsers by webSocketViewModel.onlineUsers.collectAsState()

    // State cho tính năng tìm kiếm
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var isSearchMode by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            sessionViewModel.fetchCurrentUser()
        }
        viewModel.loadConversations()
    }

    // Hàm gọi search
    val performSearch = {
        keyboardController?.hide()
        if (searchInput.isNotBlank()) {
            viewModel.searchConversations(searchInput.trim())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F0F4)) // Màu nền chung của màn hình
    ) {
        ConversationHeader(
            onBackClick = onBackClick,
            isSearchMode = isSearchMode,
            searchInput = searchInput,
            onSearchInputChange = { searchInput = it },
            onSearchModeChange = { active ->
                isSearchMode = active
                if (!active) {
                    searchInput = ""
                    viewModel.clearSearch()
                }
            },
            onSearchSubmit = performSearch
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // HIỂN THỊ KHI ĐANG Ở CHẾ ĐỘ TÌM KIẾM
            if (isSearchMode) {
                // Background che đi list cũ
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F0F4))
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(searchResults) { conv ->
                                val partner = conv.participants.find { it.id != currentUser?.id }
                                    ?: conv.participants.firstOrNull()
                                val isPartnerOnline = partner?.id?.let { onlineUsers.contains(it) } ?: partner?.isOnline ?: false

                                ConversationItem(
                                    conversation = conv,
                                    partner = partner,
                                    isOnline = isPartnerOnline,
                                    currentUserId = currentUser?.id,
                                    onClick = {
                                        onNavigateToChat(
                                            conv.id,
                                            partner?.id ?: "",
                                            partner?.displayName ?: "Unknown",
                                            partner?.avatarUrl ?: ""
                                        )
                                    }
                                )
                            }
                        }
                    } else if (searchInput.isNotBlank()) {
                        Text(
                            text = "Không tìm thấy kết quả nào",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 32.dp),
                            color = Color.Gray
                        )
                    }
                }
            }
            // HIỂN THỊ LIST BÌNH THƯỜNG KHI KHÔNG TÌM KIẾM
            else {
                if ((uiState.isLoading && uiState.conversations.isEmpty()) || currentUser == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                } else if (uiState.error != null && uiState.conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error ?: "Đã có lỗi xảy ra", color = Color.Red)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.conversations) { conv ->
                            val partner = conv.participants.find { it.id != currentUser?.id }
                                ?: conv.participants.firstOrNull()
                            val isPartnerOnline = partner?.id?.let { onlineUsers.contains(it) } ?: partner?.isOnline ?: false

                            ConversationItem(
                                conversation = conv,
                                partner = partner,
                                isOnline = isPartnerOnline,
                                currentUserId = currentUser?.id,
                                onClick = {
                                    onNavigateToChat(
                                        conv.id,
                                        partner?.id ?: "",
                                        partner?.displayName ?: "Unknown",
                                        partner?.avatarUrl ?: ""
                                    )
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
private fun ConversationHeader(
    onBackClick: () -> Unit,
    isSearchMode: Boolean,
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    onSearchModeChange: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (!isSearchMode) {
            // HIỂN THỊ HEADER BÌNH THƯỜNG
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Trò chuyện",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF131313),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Nút bấm giả làm ô tìm kiếm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5EDE2))
                    .clickable { onSearchModeChange(true) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tìm kiếm",
                    color = Color(0xFF9B948C),
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF4B4B4B),
                    modifier = Modifier.size(27.dp)
                )
            }
        } else {
            // HIỂN THỊ Ô TÌM KIẾM ACTIVE Ở TRÊN CÙNG
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSearchModeChange(false) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                TextField(
                    value = searchInput,
                    onValueChange = onSearchInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    placeholder = {
                        Text("Nhập tên bạn bè...", color = Color(0xFF9B948C), fontSize = 15.sp)
                    },
                    trailingIcon = {
                        IconButton(onClick = onSearchSubmit) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF4B4B4B)
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5EDE2),
                        unfocusedContainerColor = Color(0xFFF5EDE2),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = OrangePrimary
                    )
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    partner: Participant?,
    isOnline: Boolean,
    currentUserId: String?,
    onClick: () -> Unit
) {
    val lastMsg = conversation.lastMessage
    val isMe = lastMsg?.sender?.id == currentUserId
    val snippetText = getSnippetText(lastMsg, isMe)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = partner?.avatarUrl ?: R.drawable.icon_user,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                        .background(OnlineGreen, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner?.displayName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = snippetText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = conversation.updateAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = if (conversation.unreadCount > 0) OrangePrimary else Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getSnippetText(lastMessage: LastMessage?, isMe: Boolean): String {
    if (lastMessage == null) return "Bắt đầu trò chuyện"
    val prefix = if (isMe) "Bạn: " else ""
    return when {
        lastMessage.isRecall -> "$prefix Đã thu hồi tin nhắn"
        lastMessage.type == "MEDIA" -> {
            // Sử dụng safe call ?. và elvis operator ?: để tránh crash nếu media list là null hoặc trống
            val firstMediaType = lastMessage.media?.firstOrNull()?.type ?: "IMAGE"
            when (firstMediaType) {
                "VIDEO" -> "$prefix Đã gửi video"
                "AUDIO" -> "$prefix Đã gửi tin nhắn thoại"
                else -> "$prefix Đã gửi hình ảnh"
            }
        }
        else -> "$prefix ${lastMessage.text}"
    }
}