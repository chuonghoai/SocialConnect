package com.example.frontend.presentation.screen.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary

// 1. Tạo Data Class Mock tạm thời
data class MockConversation(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val isOnline: Boolean
)

// 2. Tạo danh sách dữ liệu giả
val mockConversations = listOf(
    MockConversation("1", "Alice", "https://i.pravatar.cc/150?u=1", "Chào bạn, khỏe không?", "10:30", 2, true),
    MockConversation("2", "Bob", "https://i.pravatar.cc/150?u=2", "Dự án tới đâu rồi?", "Hôm qua", 0, false),
    MockConversation("3", "Charlie", "https://i.pravatar.cc/150?u=3", "Okay, hẹn gặp lại nha.", "Th 2", 1, true),
    MockConversation("4", "David", "https://i.pravatar.cc/150?u=4", "Cảm ơn nhé!", "20/02", 0, false),
    MockConversation("5", "Eve", "https://i.pravatar.cc/150?u=5", "Gửi file cho mình nha, đang cần gấp", "19/02", 5, true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onBackClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        TopAppBar(
            title = { Text("Đoạn chat", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // Danh sách hiển thị
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(mockConversations) { conv ->
                ConversationItem(conversation = conv)
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: MockConversation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Chuyển đến màn hình chat chi tiết */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Chấm xanh Online
        Box {
            AsyncImage(
                model = conversation.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.background, CircleShape) // Viền trắng/đen tùy theme
                        .padding(2.dp)
                        .background(OnlineGreen, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tên & Tin nhắn gần nhất
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Thời gian & Badge đếm số tin nhắn chưa đọc
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = conversation.time,
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
                Spacer(modifier = Modifier.size(20.dp)) // Giữ layout ko bị giật
            }
        }
    }
}