package com.example.frontend.presentation.screen.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.AuthUseCase.RegisterUseCase
import com.example.frontend.domain.usecase.AuthUseCase.SendOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun setEmail(v: String) { _uiState.value = _uiState.value.copy(email = v) }
    fun setPassword(v: String) { _uiState.value = _uiState.value.copy(password = v) }
    fun setConfirmPassword(v: String) { _uiState.value = _uiState.value.copy(confirmPassword = v) }
    fun updateOtp(v: String) { _uiState.value = _uiState.value.copy(otp = v) }

    fun sendOtp(onSendOtpClick: () -> Unit) {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.value = state.copy(loading = false, error = "Vui lòng nhập đầy đủ thông tin")
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(loading = false, error = "Mật khẩu không khớp")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val res = sendOtpUseCase(state.email, "REGISTER")) {
                is ApiResult.Success -> onSendOtpClick()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = res.message.ifBlank { "Lỗi gửi OTP" }
                )
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun register(onRegisterClick: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val res = registerUseCase(state.email, state.password, state.otp)) {
                is ApiResult.Success -> onRegisterClick()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = res.message.ifBlank { "Đăng ký thất bại" }
                )
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}