package com.example.frontend.core.util

import android.net.Uri
import android.util.Log
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.CreatePostUseCase
import com.example.frontend.ui.component.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    companion object {
        private const val TAG = "PostUploadManager"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    private val _postCreatedTick = MutableStateFlow(0L)
    val postCreatedTick: StateFlow<Long> = _postCreatedTick.asStateFlow()

    private val _postCreatedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val postCreatedEvents: SharedFlow<Unit> = _postCreatedEvents.asSharedFlow()

    fun notifyPostChanged() {
        _postCreatedEvents.tryEmit(Unit)
    }

    fun uploadPost(content: String, visibility: String, uris: List<Uri>) {
        applicationScope.launch {
            _uploadState.value = UploadState(isUploading = true, progressText = "Dang chuan bi...")

            val mediaIdsToSave = mutableListOf<String>()

            if (uris.isNotEmpty()) {
                for ((index, uri) in uris.withIndex()) {
                    _uploadState.value = UploadState(
                        isUploading = true,
                        progressText = "Dang tai file ${index + 1}/${uris.size}..."
                    )

                    when (val uploadRes = uploadMediaUseCase(uri)) {
                        is ApiResult.Success -> mediaIdsToSave.add(uploadRes.data)
                        is ApiResult.Error -> {
                            _uploadState.value = UploadState(isUploading = false)
                            notificationManager.showMessage(
                                "Loi upload file: ${uploadRes.message}",
                                NotificationType.ERROR
                            )
                            return@launch
                        }
                    }
                }
            }

            _uploadState.value = UploadState(isUploading = true, progressText = "Dang hoan tat bai viet...")

            Log.d(
                TAG,
                "createPost payload: picked=${uris.size}, uploadedIds=${mediaIdsToSave.size}, ids=$mediaIdsToSave"
            )

            when (val postRes = createPostUseCase(content, visibility, mediaIdsToSave.ifEmpty { null })) {
                is ApiResult.Success -> {
                    _uploadState.value = UploadState(isUploading = false)
                    notifyPostChanged()
                    notificationManager.showMessage("Dang bai viet thanh cong!", NotificationType.SUCCESS)
                }

                is ApiResult.Error -> {
                    _uploadState.value = UploadState(isUploading = false)
                    notificationManager.showMessage("Loi dang bai: ${postRes.message}", NotificationType.ERROR)
                }
            }
        }
    }
}
