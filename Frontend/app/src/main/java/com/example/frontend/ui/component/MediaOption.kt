package com.example.frontend.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.frontend.R
import com.example.frontend.core.util.MediaDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Định nghĩa màu đen và trắng chuyên biệt
private val BlackColor = Color(0xFF000000)
private val WhiteColor = Color(0xFFFFFFFF)
private val SeparatorColor = Color(0xFF222222)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaOptionBottomSheet(
    mediaUrl: String?,
    isVideo: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onDismissRequest: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { MediaDownloadManager(context) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = BlackColor,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlackColor)
                .padding(bottom = 32.dp, top = 16.dp)
        ) {
            // Title
            Text(
                text = "Tùy chọn",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = WhiteColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Divider(color = SeparatorColor, thickness = 1.dp)

            // Download
            MediaOptionItem(
                icon = Icons.Outlined.FileDownload,
                title = "Tải xuống",
                onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissRequest()
                        }
                    }
                    Toast.makeText(context, "Đang tải xuống...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        val result = downloadManager.downloadAndSaveMedia(mediaUrl, isVideo)
                        result.onSuccess { msg ->
                            Toast.makeText(context, "Tải xuống thành công", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "Lỗi tải xuống!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // Share
            MediaOptionItem(
                icon = Icons.Outlined.Share,
                title = "Chia sẻ",
                onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissRequest()
                        }
                    }
                    onShareClick()
                }
            )
        }
    }
}

@Composable
private fun MediaOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WhiteColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = WhiteColor
        )
    }
}