package com.example.frontend.core.util

import com.example.frontend.ui.component.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationState(
    val isVisible: Boolean = false,
    val message: String = "",
    val type: NotificationType = NotificationType.SUCCESS
)

@Singleton
class AppNotificationManager @Inject constructor() {

    private val _notification = MutableStateFlow(NotificationState())
    val notification: StateFlow<NotificationState> = _notification.asStateFlow()

    fun showMessage(message: String, type: NotificationType) {
        _notification.value = NotificationState(
            isVisible = true,
            message = message,
            type = type
        )
    }

    fun clear() {
        _notification.value = _notification.value.copy(isVisible = false)
    }
}