package com.example.frontend.presentation.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.TokenProvider
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.domain.usecase.AuthUseCase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val tokenProvider: TokenProvider,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setUsername(v: String) { _uiState.value = _uiState.value.copy(username = v) }
    fun setPassword(v: String) { _uiState.value = _uiState.value.copy(password = v) }

    fun checkAlreadyLoggedIn(onLoggedIn: () -> Unit) {
        viewModelScope.launch {
            val token = tokenProvider.getAccessToken()
            if (!token.isNullOrBlank()) onLoggedIn()
            delay(200);
            webSocketManager.connect();
        }
    }

    fun login(onLoggedIn: () -> Unit) {
        val u = _uiState.value.username
        val p = _uiState.value.password

        if (u.isBlank() || p.isBlank()) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = when {
                    u.isBlank() && p.isBlank() -> "Vui lòng nhập tên đăng nhập và mật khẩu"
                    u.isBlank() -> "Vui lòng nhập tên đăng nhập"
                    else -> "Vui lòng nhập mật khẩu"
                }
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val res = loginUseCase(u, p)) {
                is ApiResult.Success -> {
                    onLoggedIn();
                    delay(200);
                    webSocketManager.connect();
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = res.message.ifBlank { "Login failed" }
                )
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}
