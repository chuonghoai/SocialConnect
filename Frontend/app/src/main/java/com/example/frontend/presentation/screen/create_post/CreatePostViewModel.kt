package com.example.frontend.presentation.screen.create_post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import com.example.frontend.domain.usecase.PostUseCase.CreatePostUseCase
import com.example.frontend.ui.component.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val notificationManager: AppNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onContentChange(text: String) {
        _uiState.update { it.copy(content = text) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun onVisibilityChange(visibility: String) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun createPost(onSuccess: (String) -> Unit) {
        val state = _uiState.value

        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var mediaIdToSave: String? = null

            if (state.selectedImageUri != null) {
                when (val uploadRes = uploadMediaUseCase(state.selectedImageUri)) {
                    is ApiResult.Success -> {
                        mediaIdToSave = uploadRes.data
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = uploadRes.message) }
                        notificationManager.showMessage("Lỗi up ảnh: ${uploadRes.message}", NotificationType.ERROR)
                        return@launch
                    }
                }
            }

            when (val postRes = createPostUseCase(state.content, state.visibility, mediaIdToSave)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val newPostId = postRes.data
                    notificationManager.showMessage("Đăng bài viết thành công!", NotificationType.SUCCESS)
                    onSuccess(newPostId)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = postRes.message) }
                    notificationManager.showMessage("Lỗi đăng bài: ${postRes.message}", NotificationType.ERROR)
                }
            }
        }
    }
}