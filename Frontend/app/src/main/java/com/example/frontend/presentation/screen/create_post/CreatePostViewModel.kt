package com.example.frontend.presentation.screen.create_post

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.frontend.core.util.AppNotificationManager
import com.example.frontend.core.util.PostEditSyncManager
import com.example.frontend.core.util.PostUploadManager
import com.example.frontend.data.store.PostDetailStore
import com.example.frontend.domain.model.Post
import com.example.frontend.ui.component.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val notificationManager: AppNotificationManager,
    private val postUploadManager: PostUploadManager,
    private val postEditSyncManager: PostEditSyncManager,
    private val postDetailStore: PostDetailStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun initialize(isEditMode: Boolean) {
        if (_uiState.value.isInitialized) return

        if (isEditMode) {
            val postToEdit = postDetailStore.editingPost
            if (postToEdit != null) {
                _uiState.value = CreatePostUiState(
                    mode = PostComposerMode.EDIT,
                    editingPostId = postToEdit.id,
                    isInitialized = true,
                    content = postToEdit.content,
                    selectedMediaUris = extractMediaUris(postToEdit),
                    visibility = "Công khai"
                )
                return
            }
        }

        _uiState.value = CreatePostUiState(
            mode = PostComposerMode.CREATE,
            isInitialized = true
        )
    }

    fun onContentChange(text: String) {
        _uiState.update { it.copy(content = text) }
    }

    fun onMediaSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedMediaUris = (it.selectedMediaUris + uris).distinct()) }
    }

    fun removeMedia(uri: Uri) {
        _uiState.update { it.copy(selectedMediaUris = it.selectedMediaUris - uri) }
    }

    fun onVisibilityChange(visibility: String) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.content.isBlank() && state.selectedMediaUris.isEmpty()) return

        if (state.mode == PostComposerMode.EDIT) {
            saveEditedPost(state, onSuccess)
        } else {
            postUploadManager.uploadPost(state.content, state.visibility, state.selectedMediaUris)
            onSuccess()
        }
    }

    private fun saveEditedPost(state: CreatePostUiState, onSuccess: () -> Unit) {
        val sourcePost = postDetailStore.editingPost ?: run {
            notificationManager.showMessage(
                "Không tìm thấy bài viết để chỉnh sửa.",
                NotificationType.ERROR
            )
            return
        }

        val updatedMediaUrls = state.selectedMediaUris.map { it.toString() }
        val updatedPost = sourcePost.copy(
            content = state.content,
            media = null,
            mediaIds = null,
            mediaUrls = updatedMediaUrls.ifEmpty { null },
            images = null,
            videos = null,
            cdnUrl = updatedMediaUrls.joinToString("|")
        )

        postDetailStore.editingPost = updatedPost
        postEditSyncManager.publish(updatedPost)

        // TODO(FE): khi BE có endpoint update post, gọi API update tại đây thay cho local update.
        notificationManager.showMessage("Đã lưu chỉnh sửa bài viết", NotificationType.SUCCESS)
        onSuccess()
    }

    private fun extractMediaUris(post: Post): List<Uri> {
        val urls = buildList {
            post.media.orEmpty().forEach { add(it.resolvedUrl()) }
            post.mediaIds.orEmpty().forEach { add(it.resolvedUrl()) }
            addAll(post.mediaUrls.orEmpty())
            addAll(post.images.orEmpty())
            addAll(post.videos.orEmpty())

            val rawCdn = post.cdnUrl.trim()
            if (rawCdn.isNotBlank()) {
                rawCdn.split("|", ",", ";", "\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { add(it) }
            }
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return urls.map(Uri::parse)
    }
}
