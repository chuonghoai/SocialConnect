package com.example.frontend.presentation.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.NotificationItem
import com.example.frontend.domain.usecase.NotificationUseCase.GetMyNotificationsUseCase
import com.example.frontend.domain.usecase.NotificationUseCase.MarkAsReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val items: List<NotificationItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getMyNotificationsUseCase: GetMyNotificationsUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications(onUpdated: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getMyNotificationsUseCase(limit = 50, offset = 0, isRead = null)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.data.items,
                            error = null
                        )
                    }
                    onUpdated?.invoke()
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun markAsRead(notificationId: Int, onUpdated: (() -> Unit)? = null) {
        val target = _uiState.value.items.firstOrNull { it.id == notificationId } ?: return
        if (target.isRead) return

        viewModelScope.launch {
            when (markAsReadUseCase(notificationId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.map { item ->
                                if (item.id == notificationId) item.copy(isRead = true) else item
                            }
                        )
                    }
                    onUpdated?.invoke()
                }

                is ApiResult.Error -> Unit
            }
        }
    }
}
