package com.example.frontend.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SavedPostUpdate(
    val postId: String,
    val isSaved: Boolean
)

@Singleton
class SavedPostSyncManager @Inject constructor() {

    private val _updates = MutableSharedFlow<SavedPostUpdate>(extraBufferCapacity = 64)
    val updates: SharedFlow<SavedPostUpdate> = _updates.asSharedFlow()

    fun publish(postId: String, isSaved: Boolean) {
        _updates.tryEmit(SavedPostUpdate(postId = postId, isSaved = isSaved))
    }
}
