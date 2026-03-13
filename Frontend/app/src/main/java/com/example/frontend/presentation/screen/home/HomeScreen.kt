package com.example.frontend.presentation.screen.home

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.domain.model.User
import com.example.frontend.ui.theme.OrangePrimary
import java.time.LocalDateTime
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import com.example.frontend.presentation.navigation.Routes
import com.example.frontend.ui.theme.OrangeLight
import kotlinx.coroutines.launch
import com.example.frontend.ui.component.PostCard
import com.example.frontend.ui.component.ScrollToTopButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User?,
    onNavigateToMessages: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    LaunchedEffect(Unit) {
        if (uiState is HomeUiState.Loading) {
            viewModel.load()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(onNavigateToMessages = onNavigateToMessages, onCreatePostClick = onCreatePostClick)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.load(isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        if (currentUser != null) CreatePostSection(currentUser, onCreatePostClick)
                    }

                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = OrangePrimary)
                                }
                            }
                        }

                        is HomeUiState.Error -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 50.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = state.message, color = Color.Red)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { viewModel.load() }) {
                                        Text("Thử lại")
                                    }
                                }
                            }
                        }

                        is HomeUiState.Success -> {
                            items(state.posts) { post ->
                                PostCard(
                                    post = post,
                                    onLikeClick = {
                                        viewModel.toggleLike(post.id)
                                    },
                                    onCommentClick = {
                                        viewModel.selectPost(post)
                                        onPostClick(post)
                                    }
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
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

// Header
@Composable
fun HomeHeader(
    onNavigateToMessages: () -> Unit = {},
    onCreatePostClick: () -> Unit = {}
) {
    Surface(
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .height(60.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AliceApp",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                style = MaterialTheme.typography.headlineMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCreatePostClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_plus),
                        contentDescription = "New Post",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onNavigateToMessages) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_message),
                        contentDescription = "Messages",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// Create new post
@Composable
fun CreatePostSection(user: User, onCreatePostClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.icon_user)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onCreatePostClick() }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Bạn đang nghĩ gì?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_image),
                contentDescription = "Add Image",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onCreatePostClick() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
