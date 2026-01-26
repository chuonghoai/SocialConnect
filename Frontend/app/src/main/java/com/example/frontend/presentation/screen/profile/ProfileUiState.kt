package com.example.frontend.presentation.screen.profile

import com.example.frontend.domain.model.User

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
