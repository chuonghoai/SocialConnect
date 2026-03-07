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

    var u = String()
    var p = String()
    var cp = String()
    var e = String()
    var otp = String()

    fun sendOtp(onSendOtpClick: () -> Unit) {
        u = _uiState.value.email
        p = _uiState.value.password
        cp = _uiState.value.confirmPassword
        e = _uiState.value.email

        if (u.isBlank() || p.isBlank() || cp.isBlank()) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = when {
                    u.isBlank() && p.isBlank() -> "Vui lòng nhập email và mật khẩu"
                    u.isBlank() -> "Vui lòng nhập tên đăng nhập"
                    p.isBlank() -> "Vui lòng nhập mật khẩu"
                    else -> "Vui lòng nhập lại mật khẩu"
                }
            )
            return
        }

        if (!u.contains("@")) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = "Email không hợp lệ"
            )
            return
        }

        if (p != cp) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = "Mật khẩu xác nhận không khớp"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val res = sendOtpUseCase(e, "REGISTER")) {
                is ApiResult.Success -> onSendOtpClick()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = res.message.ifBlank { "Send OTP failed" }
                )
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun register(onRegisterClick: () -> Unit) {
        otp = _uiState.value.otp
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val res = registerUseCase(e, p, otp)) {
                is ApiResult.Success -> onRegisterClick()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = res.message.ifBlank { "Register failed" }
                )
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}