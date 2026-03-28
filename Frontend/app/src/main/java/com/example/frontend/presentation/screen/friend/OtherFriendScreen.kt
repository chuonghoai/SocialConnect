package com.example.frontend.presentation.screen.friend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.frontend.R
import com.example.frontend.domain.model.FriendRecipient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherFriendScreen(
    onBack: () -> Unit,
    onAvatarClick: ((String) -> Unit)? = null,
    onNavigateToChat: (String, String, String, String?) -> Unit,
    viewModel: OtherFriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToChatEvent.collect { event ->
            onNavigateToChat(
                event.conversationId,
                event.partnerId,
                event.partnerName,
                event.partnerAvatarUrl
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bạn bè") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Lỗi: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.friends) { friend ->
                        FriendListItem(
                            friend = friend,
                            onAvatarClick = onAvatarClick,
                            onChatClick = { viewModel.startChatWithFriend(friend) }
                        )
                    }
                }
            }
        }
    }
}

// Reuse `FriendListItem` defined in MyFriendScreen.kt to avoid duplicate symbols
