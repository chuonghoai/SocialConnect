package com.example.frontend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.frontend.R
import com.example.frontend.domain.model.Post
import com.example.frontend.presentation.screen.share.SharePostSubmitData

data class ShareDropdownOption(
    val id: String,
    val label: String
)

data class ShareFriendItem(
    val id: String,
    val name: String,
    val avatarUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostCaptionDialog(
    post: Post,
    currentUserId: String,
    currentUserName: String,
    currentUserAvatarUrl: String?,
    postTargets: List<ShareDropdownOption>,
    privacyOptions: List<ShareDropdownOption>,
    friends: List<ShareFriendItem>,
    isFriendsLoading: Boolean,
    friendsError: String?,
    onRetryLoadFriends: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmShare: (SharePostSubmitData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetMaxHeight = (screenHeight * 0.58f).coerceAtLeast(360.dp)

    var shareText by remember(post.id) { mutableStateOf("") }
    var selectedTargetId by remember(post.id) { mutableStateOf(postTargets.firstOrNull()?.id.orEmpty()) }
    var selectedPrivacyId by remember(post.id) { mutableStateOf(privacyOptions.firstOrNull()?.id.orEmpty()) }
    var selectedFriendIds by remember(post.id) { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        containerColor = Color(0xFFF2F3F8),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFB6BCC8))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight * 0.5f, max = sheetMaxHeight)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ShareComposer(
                        userName = currentUserName,
                        userAvatarUrl = currentUserAvatarUrl,
                        postTargets = postTargets,
                        selectedTargetId = selectedTargetId,
                        onTargetSelected = { selectedTargetId = it },
                        privacyOptions = privacyOptions,
                        selectedPrivacyId = selectedPrivacyId,
                        onPrivacySelected = { selectedPrivacyId = it },
                        shareText = shareText,
                        onShareTextChange = { shareText = it },
                        onClose = onDismiss,
                        onShareNow = {
                            onConfirmShare(
                                SharePostSubmitData(
                                    shareText = shareText.trim(),
                                    target = selectedTargetId,
                                    privacy = selectedPrivacyId,
                                    selectedFriendIds = selectedFriendIds.toList(),
                                    currentUserId = currentUserId,
                                    postId = post.id
                                )
                            )
                        }
                    )
                }

                item {
                    FriendRecipientList(
                        friends = friends,
                        selectedFriendIds = selectedFriendIds,
                        isLoading = isFriendsLoading,
                        errorMessage = friendsError,
                        onRetry = onRetryLoadFriends,
                        onToggle = { friendId ->
                            selectedFriendIds = if (selectedFriendIds.contains(friendId)) {
                                selectedFriendIds - friendId
                            } else {
                                selectedFriendIds + friendId
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareComposer(
    userName: String,
    userAvatarUrl: String?,
    postTargets: List<ShareDropdownOption>,
    selectedTargetId: String,
    onTargetSelected: (String) -> Unit,
    privacyOptions: List<ShareDropdownOption>,
    selectedPrivacyId: String,
    onPrivacySelected: (String) -> Unit,
    shareText: String,
    onShareTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onShareNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = userAvatarUrl,
                    contentDescription = "Current user avatar",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.icon_user)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShareSelectorPill(
                            options = postTargets,
                            selectedId = selectedTargetId,
                            onSelected = onTargetSelected
                        )
                        ShareSelectorPill(
                            options = privacyOptions,
                            selectedId = selectedPrivacyId,
                            onSelected = onPrivacySelected,
                            leadingIcon = Icons.Outlined.Lock
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "\u0110\u00f3ng popup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = shareText,
                onValueChange = onShareTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                placeholder = {
                    Text(
                        text = "H\u00e3y n\u00f3i g\u00ec \u0111\u00f3 v\u1ec1 n\u1ed9i dung n\u00e0y...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF7F8FA),
                    unfocusedContainerColor = Color(0xFFF7F8FA)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEmotions,
                            contentDescription = "C\u1ea3m x\u00fac",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = "G\u1eafn th\u1ebb b\u1ea1n b\u00e8",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onShareNow,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Chia s\u1ebb ngay",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareSelectorPill(
    options: List<ShareDropdownOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    leadingIcon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = options.firstOrNull { it.id == selectedId }
    val selectedLabel = selectedItem?.label ?: ""
    val enabled = options.isNotEmpty()

    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE7ECF3),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) {
                    if (options.size > 1) {
                        expanded = true
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FriendRecipientList(
    friends: List<ShareFriendItem>,
    selectedFriendIds: Set<String>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "G\u1eedi cho b\u1ea1n b\u00e8",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading -> {
                FriendListLoading()
            }

            errorMessage != null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text("Th\u1eed l\u1ea1i")
                        }
                    }
                }
            }

            friends.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Ch\u01b0a c\u00f3 b\u1ea1n b\u00e8",
                        modifier = Modifier.padding(vertical = 18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = friends, key = { it.id }) { friend ->
                        val isSelected = selectedFriendIds.contains(friend.id)
                        FriendRecipientItem(
                            friend = friend,
                            selected = isSelected,
                            onClick = { onToggle(friend.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendListLoading() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(5) {
            Column(
                modifier = Modifier.width(82.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E5EC))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE2E5EC))
                )
            }
        }
    }
}

@Composable
private fun FriendRecipientItem(
    friend: ShareFriendItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = friend.avatarUrl,
                contentDescription = friend.name,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color(0xFF1877F2) else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.icon_user)
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "\u0110\u00e3 ch\u1ecdn",
                    tint = Color(0xFF1877F2),
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = friend.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )

        if (selected) {
            Text(
                text = "\u0110\u00e3 ch\u1ecdn",
                color = Color(0xFF1877F2),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
