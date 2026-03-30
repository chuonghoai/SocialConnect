package com.example.frontend.presentation.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.ConversationUseCase.GetUnseenCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val getUnseenCountUseCase: GetUnseenCountUseCase
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun refreshUnreadCount() {
        viewModelScope.launch {
            when (val result = getUnseenCountUseCase()) {
                is ApiResult.Success -> _unreadCount.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }
}