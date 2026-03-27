package com.example.frontend.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.frontend.core.util.MediaDownloadManager

@Composable
fun MediaOptionDialog(
    mediaUrl: String?,
    isVideo: Boolean,
    onDismiss: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Khởi tạo service download
    val downloadManager = remember { MediaDownloadManager(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tùy chọn") },
        text = {
            Column {
                // Download
                TextButton(onClick = {
                    onDismiss()
                    Toast.makeText(context, "Đang tải xuống...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        val result = downloadManager.downloadAndSaveMedia(mediaUrl, isVideo)
                        result.onSuccess { msg ->
                            Toast.makeText(context, "Tải xuống thành công", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "Lỗi tải xuống!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tải xuống")
                    }
                }

                // Share
                TextButton(onClick = {
                    onShareClick()
                    onDismiss()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chia sẻ")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}