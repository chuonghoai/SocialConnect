package com.example.frontend.presentation.screen.conversation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.model.Participant
import com.example.frontend.presentation.viewmodel.SessionViewModel
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary
import com.example.frontend.presentation.viewmodel.WebSocketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onBackClick: () -> Unit = {},
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: ConversationViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    webSocketViewModel: WebSocketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by sessionViewModel.currentUser.collectAsState()
    val onlineUsers by webSocketViewModel.onlineUsers.collectAsState()

    LaunchedEffect(Unit) {
        Log.d("ConversationScreen", "LaunchedEffect: Loading data...")
        if (currentUser == null) {
            Log.d("ConversationScreen", "CurrentUser is null, fetching...")
            sessionViewModel.fetchCurrentUser()
        }
        viewModel.loadConversations()
    }

    Log.d("ConversationScreen", "State: isLoading=${uiState.isLoading}, conversations=${uiState.conversations.size}, currentUser=${currentUser?.username}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F0F4))
    ) {
        ConversationHeader(onBackClick = onBackClick)

        if ((uiState.isLoading && uiState.conversations.isEmpty()) || currentUser == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentUser == null) "Đang tải thông tin người dùng..." else "Đang tải tin nhắn...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else if (uiState.error != null && uiState.conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Đã có lỗi xảy ra", color = Color.Red)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.conversations) { conv ->
                    // Lọc lấy partner (người có ID khác với currentUser)
                    val partner = conv.participants.find { it.id != currentUser?.id }
                        ?: conv.participants.firstOrNull()
                    val isPartnerOnline = partner?.id?.let { onlineUsers.contains(it) } ?: partner?.isOnline ?: false

                    ConversationItem(
                        conversation = conv,
                        partner = partner,
                        isOnline = isPartnerOnline,
                        onClick = { 
                            onNavigateToChat(
                                conv.id, 
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

@Composable
private fun ConversationHeader(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
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

            Box {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Map",
                        tint = Color(0xFF1C1C1C),
                        modifier = Modifier.size(31.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .size(17.dp)
                        .background(Color(0xFFB6191D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF5EDE2))
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
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    partner: Participant?,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val lastMsg = conversation.lastMessage
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Chấm xanh Online
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
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .padding(2.dp)
                        .background(OnlineGreen, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tên & Tin nhắn gần nhất
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner?.displayName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (lastMsg?.isRecall == true) "Tin nhắn đã bị thu hồi" else lastMsg?.text ?: "Bắt đầu trò chuyện",
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge đếm số tin nhắn chưa đọc
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
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}
