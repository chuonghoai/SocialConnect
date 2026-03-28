package com.example.frontend.presentation.screen.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRecipient
import com.example.frontend.domain.usecase.FriendUseCase.GetMyFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyFriendUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendRecipient> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyFriendViewModel @Inject constructor(
    private val getMyFriendsUseCase: GetMyFriendsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyFriendUiState(isLoading = true))
    val uiState: StateFlow<MyFriendUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = MyFriendUiState(isLoading = true)

            when (val result = getMyFriendsUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = MyFriendUiState(
                        isLoading = false,
                        friends = result.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = MyFriendUiState(
                        isLoading = false,
                        error = result.message ?: "Không thể tải danh sách bạn bè"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadFriends()
    }
}
