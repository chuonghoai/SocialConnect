package com.example.frontend.core.util

import android.net.Uri
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.CreatePostUseCase
import com.example.frontend.ui.component.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class UploadState(
    val isUploading: Boolean = false,
    val progressText: String = ""
)

@Singleton
class PostUploadManager @Inject constructor(
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val notificationManager: AppNotificationManager
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun uploadPost(content: String, visibility: String, uris: List<Uri>) {
        applicationScope.launch {
            _uploadState.value = UploadState(isUploading = true, progressText = "Đang chuẩn bị...")

            val mediaIdsToSave = mutableListOf<String>()

            if (uris.isNotEmpty()) {
                for ((index, uri) in uris.withIndex()) {
                    _uploadState.value = UploadState(
                        isUploading = true,
                        progressText = "Đang tải file ${index + 1}/${uris.size}..."
                    )

                    when (val uploadRes = uploadMediaUseCase(uri)) {
                        is ApiResult.Success -> mediaIdsToSave.add(uploadRes.data)
                        is ApiResult.Error -> {
                            _uploadState.value = UploadState(isUploading = false) // Tắt loading
                            notificationManager.showMessage("Lỗi up file: ${uploadRes.message}", NotificationType.ERROR)
                            return@launch
                        }
                    }
                }
            }

            _uploadState.value = UploadState(isUploading = true, progressText = "Đang hoàn tất bài viết...")

            when (val postRes = createPostUseCase(content, visibility, mediaIdsToSave.ifEmpty { null })) {
                is ApiResult.Success -> {
                    _uploadState.value = UploadState(isUploading = false)
                    notificationManager.showMessage("Đăng bài viết thành công!", NotificationType.SUCCESS)
                }
                is ApiResult.Error -> {
                    _uploadState.value = UploadState(isUploading = false)
                    notificationManager.showMessage("Lỗi đăng bài: ${postRes.message}", NotificationType.ERROR)
                }
            }
        }
    }
}