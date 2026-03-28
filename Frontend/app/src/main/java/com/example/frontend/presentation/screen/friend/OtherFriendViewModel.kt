package com.example.frontend.presentation.screen.friend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRecipient
import com.example.frontend.domain.usecase.FriendUseCase.GetUserFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtherFriendUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendRecipient> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OtherFriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserFriendsUseCase: GetUserFriendsUseCase
) : ViewModel() {

    private val targetUserId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _uiState = MutableStateFlow(OtherFriendUiState(isLoading = true))
    val uiState: StateFlow<OtherFriendUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            if (targetUserId.isBlank()) {
                _uiState.value = OtherFriendUiState(isLoading = false, error = "Không xác định người dùng")
                return@launch
            }

            _uiState.value = OtherFriendUiState(isLoading = true)

            when (val result = getUserFriendsUseCase(targetUserId)) {
                is ApiResult.Success -> {
                    _uiState.value = OtherFriendUiState(isLoading = false, friends = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = OtherFriendUiState(isLoading = false, error = result.message ?: "Không thể tải danh sách bạn bè")
                }
            }
        }
    }

    fun refresh() {
        loadFriends()
    }
}
