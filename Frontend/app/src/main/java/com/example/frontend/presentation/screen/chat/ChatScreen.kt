package com.example.frontend.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary

// 1. Dữ liệu giả (Mock Data)
data class MockMessage(val id: String, val text: String, val time: String, val isMine: Boolean)

val mockMessages = listOf(
    MockMessage("1", "Chào bạn!", "10:00", false),
    MockMessage("2", "Chào Alice, dạo này khỏe không?", "10:05", true),
    MockMessage("3", "Mình khỏe, cảm ơn bạn. Dự án tới đâu rồi?", "10:06", false),
    MockMessage("4", "Đang tiến triển tốt nhé! Giao diện gần xong rồi.", "10:10", true),
    MockMessage("5", "Tuyệt vời, cố lên nha!", "10:12", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String, // ID truyền từ màn hình ConversationList
    onBackClick: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- TOP BAR ---
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AsyncImage(
                            model = "https://i.pravatar.cc/150?u=$conversationId",
                            contentDescription = "Avatar",
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(2.dp)
                                .background(OnlineGreen, CircleShape)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Người dùng $conversationId",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Đang hoạt động", color = OnlineGreen, fontSize = 12.sp)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Phone, "Call", tint = OrangePrimary) }
                IconButton(onClick = {}) { Icon(Icons.Default.Videocam, "Video", tint = OrangePrimary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // --- DANH SÁCH TIN NHẮN ---
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true // Lật ngược list để cuộn từ dưới lên (chuẩn UX App chat)
        ) {
            // Đảo ngược danh sách vì reverseLayout = true
            items(mockMessages.reversed()) { msg ->
                MessageBubble(msg)
                Spacer(Modifier.height(12.dp))
            }
        }

        // --- KHUNG NHẬP TIN NHẮN ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhắn tin...", color = Color.Gray) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { /* Todo: Gửi tin nhắn */ messageText = "" },
                modifier = Modifier.size(48.dp).background(OrangePrimary, CircleShape),
                enabled = messageText.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}

// Bong bóng tin nhắn trái/phải
@Composable
fun MessageBubble(message: MockMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f) // Không cho tin nhắn quá dài tràn viền
                .background(
                    color = if (message.isMine) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isMine) 16.dp else 4.dp,
                        bottomEnd = if (message.isMine) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
                Text(
                    text = message.text,
                    color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.time,
                    color = if (message.isMine) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}