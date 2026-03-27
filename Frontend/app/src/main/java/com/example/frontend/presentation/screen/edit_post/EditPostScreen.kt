package com.example.frontend.presentation.screen.edit_post

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.frontend.R
import com.example.frontend.domain.model.PostMedia
import com.example.frontend.domain.model.User
import com.example.frontend.presentation.screen.create_post.VisibilityDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    currentUser: User?,
    initialContent: String,
    initialVisibility: String,
    initialMedia: List<PostMedia>,
    onBackClick: () -> Unit,
    onComplete: (
        content: String,
        visibility: String,
        keptExistingMedia: List<PostMedia>,
        newMediaUris: List<Uri>
    ) -> Unit
) {
    val normalizedInitialMedia = remember(initialMedia) {
        initialMedia.mapNotNull { media ->
            val resolvedUrl = media.resolvedUrl().trim()
            if (resolvedUrl.isBlank()) null else media.copy(cdnUrl = resolvedUrl)
        }
    }

    var content by rememberSaveable(initialContent) { mutableStateOf(initialContent) }
    var visibility by rememberSaveable(initialVisibility) {
        mutableStateOf(initialVisibility.ifBlank { "Công khai" })
    }
    var existingMedia by remember(normalizedInitialMedia) { mutableStateOf(normalizedInitialMedia) }
    var newMediaUris by remember(normalizedInitialMedia) { mutableStateOf(emptyList<Uri>()) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
        onResult = { uris ->
            newMediaUris = (newMediaUris + uris).distinct()
        }
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }

                    AsyncImage(
                        model = currentUser?.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.icon_user)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentUser?.displayName ?: "Người dùng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            Column {
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VisibilityDropdown(
                        selectedOption = visibility,
                        onOptionSelected = { visibility = it }
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_image),
                            contentDescription = "Thêm ảnh/video",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onComplete(
                                content.trim(),
                                visibility,
                                existingMedia,
                                newMediaUris
                            )
                        },
                        enabled = content.isNotBlank() || existingMedia.isNotEmpty() || newMediaUris.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4FB3)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Hoàn tất", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TextField(
                value = content,
                onValueChange = { value -> content = value },
                placeholder = {
                    Text(
                        "Bạn đang nghĩ gì?",
                        fontSize = 20.sp,
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth()
            )

            if (existingMedia.isNotEmpty() || newMediaUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (existingMedia.isNotEmpty()) {
                existingMedia.forEach { media ->
                    val url = media.resolvedUrl()
                    EditableMediaPreview(
                        model = url,
                        isVideo = media.kind.orEmpty().uppercase().contains("VIDEO"),
                        onRemove = {
                            existingMedia = existingMedia.filterNot {
                                val idMatches = it.publicId == media.publicId && !media.publicId.isNullOrBlank()
                                val urlMatches = it.resolvedUrl() == media.resolvedUrl()
                                idMatches || urlMatches
                            }
                        }
                    )
                }
            }

            if (newMediaUris.isNotEmpty()) {
                newMediaUris.forEach { uri ->
                    EditableMediaPreview(
                        model = uri,
                        isVideo = isVideoUri(context, uri),
                        onRemove = {
                            newMediaUris = newMediaUris - uri
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableMediaPreview(
    model: Any,
    isVideo: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageModel = if (isVideo) {
        ImageRequest.Builder(context)
            .data(model)
            .decoderFactory(VideoFrameDecoder.Factory())
            .crossfade(true)
            .build()
    } else {
        model
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "Media preview",
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 350.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .padding(start = 8.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Xóa media",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun isVideoUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
    if (mimeType.startsWith("video/")) return true

    val raw = uri.toString().lowercase()
    return VIDEO_EXTENSIONS.any { ext ->
        raw.contains(".$ext")
    }
}

private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "m3u8", "mkv", "avi", "3gp", "flv")
