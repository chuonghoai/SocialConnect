package com.example.frontend.presentation.screen.friendrequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRequestItem
import com.example.frontend.domain.usecase.FriendUseCase.AcceptFriendRequestUseCase
import com.example.frontend.domain.usecase.FriendUseCase.GetFriendRequestsUseCase
import com.example.frontend.domain.usecase.FriendUseCase.RejectFriendRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendRequestsUiState(
    val isLoading: Boolean = false,
    val items: List<FriendRequestItem> = emptyList(),
    val handlingUserIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class FriendRequestsViewModel @Inject constructor(
    private val getFriendRequestsUseCase: GetFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestsUiState(isLoading = true))
    val uiState: StateFlow<FriendRequestsUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getFriendRequestsUseCase()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.data,
                            error = null
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message.ifBlank { "Khong the tai loi moi ket ban" }
                        )
                    }
                }
            }
        }
    }

    fun accept(friendId: String) {
        if (friendId.isBlank()) return
        if (_uiState.value.handlingUserIds.contains(friendId)) return

        viewModelScope.launch {
            _uiState.update { it.copy(handlingUserIds = it.handlingUserIds + friendId) }
            when (acceptFriendRequestUseCase(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            handlingUserIds = it.handlingUserIds - friendId,
                            items = it.items.filterNot { item -> item.fromUserId == friendId }
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            handlingUserIds = it.handlingUserIds - friendId
                        )
                    }
                }
            }
        }
    }

    fun reject(friendId: String) {
        if (friendId.isBlank()) return
        if (_uiState.value.handlingUserIds.contains(friendId)) return

        viewModelScope.launch {
            _uiState.update { it.copy(handlingUserIds = it.handlingUserIds + friendId) }
            when (rejectFriendRequestUseCase(friendId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            handlingUserIds = it.handlingUserIds - friendId,
                            items = it.items.filterNot { item -> item.fromUserId == friendId }
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            handlingUserIds = it.handlingUserIds - friendId
                        )
                    }
                }
            }
        }
    }
}
