package com.example.frontend.core.util

import com.example.frontend.domain.model.Post
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PostEditedUpdate(
    val post: Post
)

@Singleton
class PostEditSyncManager @Inject constructor() {

    private val _updates = MutableSharedFlow<PostEditedUpdate>(extraBufferCapacity = 32)
    val updates: SharedFlow<PostEditedUpdate> = _updates.asSharedFlow()

    fun publish(post: Post) {
        _updates.tryEmit(PostEditedUpdate(post = post))
    }
}
