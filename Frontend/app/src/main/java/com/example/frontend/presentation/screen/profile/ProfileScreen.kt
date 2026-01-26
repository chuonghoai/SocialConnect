package com.example.frontend.presentation.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onProfileUpdate: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    userAvatarUrl: String?
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                actions = {
                    TextButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Retry") }
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
