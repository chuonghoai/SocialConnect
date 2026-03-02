package com.example.frontend.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary

// Dữ liệu giả
data class MockMessage(val id: String, val text: String, val time: String, val isMine: Boolean)

val mockMessages = listOf(
    MockMessage("1", "Nói gì đi bro", "10:58", true),
    MockMessage("2", "Nhất sinh nhị\nNhị sinh tam\nTam sinh vạn vật\nBro đi qua lâm vân tất hóa\như vô", "10:59", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String = "Người dùng",
    conversationAvatarUrl: String? = null,
    onBackClick: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE9E9E9))
    ) {
        TopCaptionBar()

        ChatTopBar(
            title = conversationName,
            avatarUrl = conversationAvatarUrl,
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "10:58",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            items(mockMessages) { msg ->
                MessageBubble(
                    message = msg,
                    incomingAvatarUrl = conversationAvatarUrl
                )
            }
        }

        ChatInputBar(
            value = messageText,
            onValueChange = { messageText = it },
            onSendClick = { messageText = "" }
        )
    }
}

@Composable
private fun TopCaptionBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color(0xFFE5E5E5))
            .border(width = 0.6.dp, color = Color(0xFFD0D0D0)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Message - Conversation",
            color = Color(0xFF8A8A8A),
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    avatarUrl: String?,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(Color.White)
            .border(width = 0.8.dp, color = Color(0xFFD3D3D3))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF212121),
                modifier = Modifier.size(24.dp)
            )
        }

        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.icon_user),
            error = painterResource(id = R.drawable.icon_user)
        )

        Box(
            modifier = Modifier
                .offset(x = (-4).dp, y = 10.dp)
                .size(8.dp)
                .background(OnlineGreen, CircleShape)
                .border(1.dp, Color.White, CircleShape)
        )

        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF111111),
                fontSize = 25.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Online",
                color = Color(0xFF6F6F6F),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Call",
                tint = Color(0xFF212121),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video",
                tint = Color(0xFF212121),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MockMessage,
    incomingAvatarUrl: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isMine) {
            AsyncImage(
                model = incomingAvatarUrl,
                contentDescription = "Sender Avatar",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.icon_user),
                error = painterResource(id = R.drawable.icon_user)
            )
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (message.isMine) Color(0xFFE8A46F) else Color(0xFFF4F4F4),
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 220.dp)
        ) {
            Text(
                text = message.text,
                color = Color(0xFF1F1F1F),
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (message.isMine) {
            Spacer(Modifier.width(8.dp))
            ChatLetterAvatar(letter = "F")
        }
    }
}

@Composable
private fun ChatLetterAvatar(letter: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8A46F))
            .border(1.dp, Color(0xFFDA8D54), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 0.8.dp, color = Color(0xFFD9D9D9))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardVoice,
                contentDescription = "Voice",
                tint = Color(0xFF5A5A5A),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE9E9E9))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF333333),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(OrangePrimary),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(Color(0xFFE1A66E))
                        )
                        Spacer(Modifier.width(6.dp))
                        if (value.isEmpty()) {
                            Text(
                                text = "",
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SentimentSatisfiedAlt,
                contentDescription = "Emoji",
                tint = Color(0xFF5A5A5A),
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(6.dp))

        IconButton(
            onClick = onSendClick,
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFF1F1F1), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "More",
                tint = Color(0xFF5A5A5A),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}