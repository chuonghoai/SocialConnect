package com.example.frontend.presentation.screen.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.Conversation
import com.example.frontend.domain.usecase.ConversationUseCase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)


@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getConversationsUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = result.data ?: emptyList()
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
