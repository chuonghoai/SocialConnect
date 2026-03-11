package com.example.frontend.presentation.screen.create_post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor() : ViewModel() {

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

    fun createPost(onSuccess: () -> Unit) {
        // TODO: Lát nữa chúng ta sẽ gọi API Upload ảnh lên Cloudinary và gọi API Create Post ở đây
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Giả lập delay gọi API
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }
}