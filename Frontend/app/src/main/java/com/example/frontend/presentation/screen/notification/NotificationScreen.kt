package com.example.frontend.presentation.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.ui.theme.OrangePrimary

// --- MOCK DATA ---
data class MockNotification(
    val id: String,
    val username: String,
    val initial: String, // Chữ cái đầu (B, A, C...)
    val actionText: String,
    val timeAgo: String,
    val isUnread: Boolean,
    val thumbnailImage: String? = null // Hình thumbnail bài viết (nếu có)
)

val mockNotificationsToday = listOf(
    MockNotification(
        id = "1",
        username = "alex photo",
        initial = "B",
        actionText = " đã thích bài viết của bạn",
        timeAgo = "2p trước",
        isUnread = true,
        thumbnailImage = "https://picsum.photos/seed/sunset/150/150" // Ảnh ví dụ
    ),
    MockNotification(
        id = "2",
        username = "alex photo",
        initial = "A",
        actionText = " đã gửi lời mời kết bạn",
        timeAgo = "2p trước",
        isUnread = false
    )
)

val mockNotificationsThisWeek = listOf(
    MockNotification(
        id = "3",
        username = "arian",
        initial = "C",
        actionText = " đã gửi lời mời kết bạn",
        timeAgo = "7 ngày trước",
        isUnread = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCF9F5)) // Màu nền hơi ngả vàng/kem nhẹ theo mockup
    ) {
        // --- HEADER ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // --- DANH SÁCH THÔNG BÁO ---
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Nhóm: Hôm nay
            item { SectionHeader("Hôm nay") }
            items(mockNotificationsToday) { notification ->
                NotificationItem(notification)
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            // Nhóm: Tuần này
            item { SectionHeader("Tuần này") }
            items(mockNotificationsThisWeek) { notification ->
                NotificationItem(notification)
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            // Footer
            item {
                Text(
                    text = "Không còn thông báo mới",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
    Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
fun NotificationItem(notification: MockNotification) {
    // Màu nền thay đổi nếu chưa đọc
    val backgroundColor = if (notification.isUnread) Color(0xFFFDF0E3) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar dạng chữ cái
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFFE88B4A), CircleShape), // Màu cam như trong mockup
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification.initial,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Nội dung text (Tên in đậm + Hành động)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                        append(notification.username)
                    }
                    withStyle(style = SpanStyle(color = Color.Black)) {
                        append(notification.actionText)
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.timeAgo,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        // 3. Thumbnail (nếu có)
        if (notification.thumbnailImage != null) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = notification.thumbnailImage,
                contentDescription = "Post Thumbnail",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}