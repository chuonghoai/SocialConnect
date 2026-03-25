package com.example.frontend.presentation.screen.edit_post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.User
import com.example.frontend.presentation.screen.create_post.VisibilityDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    currentUser: User?,
    initialContent: String,
    initialVisibility: String,
    onBackClick: () -> Unit,
    onComplete: (content: String, visibility: String) -> Unit
) {
    var content by rememberSaveable(initialContent) { mutableStateOf(initialContent) }
    var visibility by rememberSaveable(initialVisibility) {
        mutableStateOf(initialVisibility.ifBlank { "Công khai" })
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }

                    AsyncImage(
                        model = currentUser?.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.icon_user)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentUser?.displayName ?: "Người dùng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            Column {
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VisibilityDropdown(
                        selectedOption = visibility,
                        onOptionSelected = { visibility = it }
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { onComplete(content.trim(), visibility) },
                        enabled = content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4FB3)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Hoàn tất", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TextField(
                value = content,
                onValueChange = { value -> content = value },
                placeholder = {
                    Text(
                        "Bạn đang nghĩ gì?",
                        fontSize = 20.sp,
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
