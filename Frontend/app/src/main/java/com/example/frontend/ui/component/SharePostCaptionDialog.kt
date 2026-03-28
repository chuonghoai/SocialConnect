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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Public
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

private val DefaultPrivacyOptions = listOf(
    ShareDropdownOption(id = "friends", label = "Bạn bè"),
    ShareDropdownOption(id = "public", label = "Công khai")
)

private val FacebookBlue = Color(0xFF1877F2)
private val SheetBackground = Color(0xFFF0F2F5)
private val PillBackground = Color(0xFFE7ECF3)
private val InputBackground = Color(0xFFF7F8FA)
private val SkeletonColor = Color(0xFFE2E5EC)

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
    val minSheetHeight = screenHeight * 0.5f
    val maxSheetHeight = (screenHeight * 0.6f).coerceAtLeast(380.dp)

    val effectivePostTargets = if (postTargets.isNotEmpty()) {
        postTargets
    } else {
        listOf(ShareDropdownOption(id = "feed", label = "Bảng feed"))
    }
    val effectivePrivacyOptions = remember(privacyOptions) {
        normalizePrivacyOptions(privacyOptions)
    }

    var shareText by remember(post.id) { mutableStateOf("") }
    var selectedTargetId by remember(post.id, effectivePostTargets) {
        mutableStateOf(effectivePostTargets.firstOrNull()?.id.orEmpty())
    }
    var selectedPrivacyId by remember(post.id, effectivePrivacyOptions) {
        mutableStateOf(effectivePrivacyOptions.firstOrNull()?.id.orEmpty())
    }
    var selectedFriendIds by remember(post.id) { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        containerColor = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 52.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFB7BEC9))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minSheetHeight, max = maxSheetHeight)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ShareComposer(
                    userName = currentUserName,
                    userAvatarUrl = currentUserAvatarUrl,
                    postTargetLabel = effectivePostTargets.firstOrNull { it.id == selectedTargetId }?.label
                        ?: "Bảng feed",
                    privacyOptions = effectivePrivacyOptions,
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

@Composable
private fun ShareComposer(
    userName: String,
    userAvatarUrl: String?,
    postTargetLabel: String,
    privacyOptions: List<ShareDropdownOption>,
    selectedPrivacyId: String,
    onPrivacySelected: (String) -> Unit,
    shareText: String,
    onShareTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onShareNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
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
                    contentDescription = "Ảnh đại diện người dùng",
                    modifier = Modifier
                        .size(54.dp)
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShareStaticPill(label = postTargetLabel)
                        SharePrivacySelectorPill(
                            options = privacyOptions,
                            selectedId = selectedPrivacyId,
                            onSelected = onPrivacySelected
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Đóng popup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = shareText,
                onValueChange = onShareTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 108.dp),
                placeholder = {
                    Text(
                        text = "Hãy nói gì đó về nội dung này...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShareNow,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Chia sẻ ngay",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareStaticPill(label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PillBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SharePrivacySelectorPill(
    options: List<ShareDropdownOption>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = options.firstOrNull { it.id == selectedId }
    val fallbackItem = options.firstOrNull()
    val currentOption = selectedItem ?: fallbackItem ?: DefaultPrivacyOptions.first()

    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PillBackground,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconForPrivacy(currentOption.id),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentOption.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White,
            shadowElevation = 10.dp,
            tonalElevation = 1.dp
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconForPrivacy(option.id),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.label)
                            Spacer(modifier = Modifier.weight(1f))
                            if (option.id == currentOption.id) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun iconForPrivacy(optionId: String): ImageVector {
    return when (optionId) {
        "public" -> Icons.Outlined.Public
        else -> Icons.Outlined.People
    }
}

private fun normalizePrivacyOptions(input: List<ShareDropdownOption>): List<ShareDropdownOption> {
    if (input.isEmpty()) return DefaultPrivacyOptions

    val mapped = input.mapNotNull { option ->
        when {
            option.id.equals("friends", ignoreCase = true) ||
                option.label.equals("Bạn bè", ignoreCase = true) -> {
                ShareDropdownOption(id = "friends", label = "Bạn bè")
            }

            option.id.equals("public", ignoreCase = true) ||
                option.label.equals("Công khai", ignoreCase = true) -> {
                ShareDropdownOption(id = "public", label = "Công khai")
            }

            else -> null
        }
    }.distinctBy { it.id }

    if (mapped.isEmpty()) return DefaultPrivacyOptions
    if (mapped.size == 1) {
        return if (mapped.first().id == "friends") {
            mapped + ShareDropdownOption(id = "public", label = "Công khai")
        } else {
            listOf(ShareDropdownOption(id = "friends", label = "Bạn bè")) + mapped
        }
    }

    return mapped
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
            text = "Gửi cho bạn bè",
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
                            Text("Thử lại")
                        }
                    }
                }
            }

            friends.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "Chưa có bạn bè",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
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
                        .background(SkeletonColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SkeletonColor)
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
                        color = if (selected) FacebookBlue else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.icon_user)
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Đã chọn",
                    tint = FacebookBlue,
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
                text = "Đã chọn",
                color = FacebookBlue,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
