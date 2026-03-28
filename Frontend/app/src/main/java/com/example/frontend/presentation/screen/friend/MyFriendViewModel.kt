package com.example.frontend.presentation.screen.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.FriendRecipient
import com.example.frontend.domain.usecase.ConversationUseCase.CreateConversationUseCase
import com.example.frontend.domain.usecase.FriendUseCase.GetMyFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyFriendUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendRecipient> = emptyList(),
    val error: String? = null
)

data class ChatNavigationEvent(
    val conversationId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatarUrl: String?
)

@HiltViewModel
class MyFriendViewModel @Inject constructor(
    private val getMyFriendsUseCase: GetMyFriendsUseCase,
    private val createConversationUseCase: CreateConversationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyFriendUiState(isLoading = true))
    val uiState: StateFlow<MyFriendUiState> = _uiState.asStateFlow()

    private val _navigateToChatEvent = MutableSharedFlow<ChatNavigationEvent>()
    val navigateToChatEvent = _navigateToChatEvent.asSharedFlow()

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

    fun startChatWithFriend(friend: FriendRecipient) {
        viewModelScope.launch {
            when (val result = createConversationUseCase(friend.id)) {
                is ApiResult.Success -> {
                    _navigateToChatEvent.emit(
                        ChatNavigationEvent(
                            conversationId = result.data.id,
                            partnerId = friend.id,
                            partnerName = friend.displayName,
                            partnerAvatarUrl = friend.avatarUrl
                        )
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = "Không thể tạo cuộc trò chuyện: ${result.message}")
                }
            }
        }
    }
}
