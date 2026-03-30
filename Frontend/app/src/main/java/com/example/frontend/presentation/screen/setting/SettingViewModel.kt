package com.example.frontend.presentation.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            webSocketManager.disconnect()
            logoutUseCase()
            onLoggedOut()
        }
    }
}
