package com.example.frontend.presentation.screen.chat

import MessageItem
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.ui.theme.OnlineGreen
import com.example.frontend.ui.theme.OrangePrimary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun ChatScreen(
    conversationId: String,
    partnerId: String,
    conversationName: String = "Người dùng",
    conversationAvatarUrl: String? = null,
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isPartnerOnline = uiState.onlineUsers.contains(partnerId)

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE9E9E9))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 66.dp)
                .consumeWindowInsets(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = 10.dp,
                            start = 12.dp,
                            end = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        reverseLayout = true
                    ) {
                        item {
                            Column {
                                AnimatedVisibility(
                                    visible = uiState.isPartnerTyping,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$conversationName đang nhập...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 42.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(uiState.messages) { msg ->
                            val isMine = msg.sender.id == uiState.currentUser?.id
                            MessageBubble(
                                message = msg,
                                isMine = isMine,
                                incomingAvatarUrl = conversationAvatarUrl,
                            )
                        }
                    }
                }
            }

            ChatInputBar(
                value = messageText,
                onValueChange = {
                    messageText = it
                    viewModel.onTyping(it)
                },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendChatMessage(conversationId, messageText)
                        messageText = ""
                    }
                },
            )
        }

        ChatTopBar(
            title = conversationName,
            avatarUrl = conversationAvatarUrl,
            isOnline = isPartnerOnline,
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                .background(if (isOnline) OnlineGreen else Color.Gray, CircleShape)
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
                text = if (isOnline) "Online" else "Offline",
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
    message: MessageItem,
    isMine: Boolean,
    incomingAvatarUrl: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine) {
                AsyncImage(
                    model = message.sender.avatarUrl ?: incomingAvatarUrl,
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
                color = if (isMine) Color(0xFFE8A46F) else Color(0xFFF4F4F4),
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
        }
        
        Text(
            text = formatMessageTime(message.createAt),
            color = Color(0xFF8A8A8A),
            fontSize = 10.sp,
            modifier = Modifier.padding(
                top = 2.dp,
                start = if (isMine) 0.dp else 42.dp,
                end = if (isMine) 42.dp else 0.dp
            )
        )
    }
}

fun formatMessageTime(isoString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoString)
        val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        outSdf.format(date!!)
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
