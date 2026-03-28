package com.example.frontend.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileScreen(
    partnerId: String,
    partnerName: String,
    partnerAvatarUrl: String?,
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tùy chọn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar
            AsyncImage(
                model = partnerAvatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, OrangePrimary, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user),
                placeholder = painterResource(R.drawable.icon_user)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tên
            Text(
                text = partnerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Row các nút thao tác
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem(
                    icon = Icons.Default.Person,
                    label = "Trang cá nhân",
                    onClick = { onNavigateToProfile(partnerId) }
                )
                ActionItem(
                    icon = Icons.Default.Call,
                    label = "Gọi thoại",
                    onClick = onVoiceCallClick
                )
                ActionItem(
                    icon = Icons.Default.Videocam,
                    label = "Gọi video",
                    onClick = onVideoCallClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            // Lịch sử đa phương tiện (UI tạm thời)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Lịch sử đa phương tiện",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có dữ liệu đa phương tiện",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}