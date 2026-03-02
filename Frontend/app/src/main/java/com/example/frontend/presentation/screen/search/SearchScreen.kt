package com.example.frontend.presentation.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.theme.OrangePrimary

// --- MOCK DATA ---
data class MockSearchUser(val id: String, val displayName: String, val username: String, val avatarUrl: String, val isFriend: Boolean)

val mockSearchUsers = listOf(
    MockSearchUser("1", "Alice Nguyen", "alicen", "https://i.pravatar.cc/150?u=1", true),
    MockSearchUser("2", "Bob Tran", "bob_tran", "https://i.pravatar.cc/150?u=2", false),
    MockSearchUser("3", "Charlie Le", "charliele99", "https://i.pravatar.cc/150?u=3", false),
    MockSearchUser("4", "David Pham", "david.p", "https://i.pravatar.cc/150?u=4", true),
    MockSearchUser("5", "Eva Hoang", "evahoang", "https://i.pravatar.cc/150?u=5", false)
)

val mockSearchPosts = listOf(
    Post("p1", "1", "Alice Nguyen", "https://i.pravatar.cc/150?u=1", "Hôm nay trời đẹp quá, đi chơi thôi mọi người ơi!", "ORIGINAL", "TEXT", "2024-03-02T10:00:00", 15, 3, 0, ""),
    Post("p2", "2", "Bob Tran", "https://i.pravatar.cc/150?u=2", "Có ai biết quán cà phê nào yên tĩnh để code ở Quận 1 không?", "ORIGINAL", "TEXT", "2024-03-01T15:30:00", 5, 12, 1, ""),
    Post("p3", "3", "Charlie Le", "https://i.pravatar.cc/150?u=3", "Vừa hoàn thành xong project cuối kỳ, nhẹ nhõm quá!!!", "ORIGINAL", "TEXT", "2024-02-28T09:15:00", 42, 5, 2, ""),
    Post("p4", "4", "David Pham", "https://i.pravatar.cc/150?u=4", "Thời tiết dạo này thất thường thật, sáng nắng chiều mưa.", "ORIGINAL", "TEXT", "2024-02-25T18:45:00", 10, 1, 0, "")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }

    // Lọc dữ liệu theo từ khóa cho cả User và Post
    val filteredUsers = mockSearchUsers.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
    }

    val filteredPosts = mockSearchPosts.filter {
        it.content.contains(searchQuery, ignoreCase = true) || it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- SEARCH BAR ---
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                placeholder = { Text("Tìm kiếm...", color = Color.Gray, fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
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
                singleLine = true
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // --- KẾT QUẢ TÌM KIẾM TỔNG HỢP ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Trường hợp không có kết quả nào cho cả User và Post
            if (filteredUsers.isEmpty() && filteredPosts.isEmpty()) {
                item { EmptyStateResult("Không tìm thấy kết quả nào phù hợp.") }
            } else {
                // 1. Render danh sách Người dùng trước
                items(filteredUsers) { user ->
                    UserSearchItem(user)
                }

                // 2. Render danh sách Bài viết tiếp theo ngay bên dưới
                items(filteredPosts) { post ->
                    PostCard(post = post)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(user: MockSearchUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Chuyển đến trang cá nhân của user này */ }
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
            Text(text = user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        // Nút Kết bạn / Hủy kết bạn
        Button(
            onClick = { /* Todo */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFriend) MaterialTheme.colorScheme.surfaceVariant else OrangePrimary,
                contentColor = if (user.isFriend) MaterialTheme.colorScheme.onSurface else Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (user.isFriend) "Bạn bè" else "Kết bạn",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyStateResult(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Gray, fontSize = 15.sp)
    }
}