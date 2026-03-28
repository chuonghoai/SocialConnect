package com.example.frontend.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.MediaHistoryItem
import com.example.frontend.domain.usecase.MessageUseCase.GetConversationMediasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatProfileUiState(
    val isLoading: Boolean = false,
    val medias: List<MediaHistoryItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatProfileViewModel @Inject constructor(
    private val getConversationMediasUseCase: GetConversationMediasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatProfileUiState())
    val uiState: StateFlow<ChatProfileUiState> = _uiState.asStateFlow()

    fun loadMedias(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getConversationMediasUseCase(conversationId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        medias = result.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
}