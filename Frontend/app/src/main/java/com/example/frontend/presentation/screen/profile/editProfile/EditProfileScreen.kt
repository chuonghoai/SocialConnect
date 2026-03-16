package com.example.frontend.presentation.screen.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.User
import com.example.frontend.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUser: User?,
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(currentUser) {
        viewModel.initData(currentUser)
    }

    // Trình khởi chạy chọn ảnh từ thư viện
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectedAvatarUri = uri
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Box chứa Avatar và Icon Camera
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                // Ưu tiên hiển thị ảnh vừa chọn, nếu không có mới dùng ảnh từ server
                AsyncImage(
                    model = viewModel.selectedAvatarUri ?: currentUser?.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, OrangePrimary, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.icon_user)
                )

                // Icon camera nhỏ ở góc
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Đổi ảnh",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = { viewModel.displayName = it },
                label = { Text("Họ và tên") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.dob,
                onValueChange = { viewModel.dob = it },
                label = { Text("Ngày sinh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.phone,
                onValueChange = { viewModel.phone = it },
                label = { Text("Số điện thoại") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    cursorColor = OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false // Email thường không tự đổi
            )

            if (viewModel.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveProfile {
                        onBackClick() // Lưu thành công -> back
                    }
                },
                enabled = !viewModel.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Lưu thay đổi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}