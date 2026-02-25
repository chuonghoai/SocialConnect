package com.example.frontend.presentation.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.User
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.component.ScrollToTopButton
import com.example.frontend.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Thử lại") }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.logout(onLoggedOut) }) { Text("Đăng xuất") }
                }
            }
            is ProfileUiState.Success -> {
                val user = state.user
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Profile tabs
                    item {
                        ProfileTopBar(onBackClick = onBackClick, onMoreClick = {})
                        ProfileInfoSection(user = user)
                        ProfileTabs()
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }

                    // User's post
                    items(user.myPosts) { post ->
                        PostCard(post = post)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                ScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(onBackClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Hồ sơ cá nhân",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(28.dp))
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
private fun ProfileInfoSection(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, OrangePrimary, CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.icon_user)
        )
        Spacer(Modifier.height(12.dp))

        // Display name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = OrangePrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))

        // Post and friend count
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = user.postCount.toString(), label = "Bài đăng")
            StatItem(count = user.friendCount.toString(), label = "Bạn bè")
        }
        Spacer(Modifier.height(16.dp))

        // Caption
        if (!user.caption.isNullOrBlank()) {
            Text(
                text = user.caption,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileTabs() {
    var selectedTabIndex by remember { mutableStateOf(0) }

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = OrangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = OrangePrimary
            )
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { selectedTabIndex = 0 },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Posts",
                    tint = if (selectedTabIndex == 0) OrangePrimary else Color.Gray
                )
            }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { selectedTabIndex = 1 },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Saved",
                    tint = if (selectedTabIndex == 1) OrangePrimary else Color.Gray
                )
            }
        )
    }
}