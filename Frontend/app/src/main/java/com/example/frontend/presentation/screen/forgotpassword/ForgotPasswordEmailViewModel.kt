package com.example.frontend.presentation.screen.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.AuthUseCase.SendOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordEmailViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordEmailUiState())
    val uiState: StateFlow<ForgotPasswordEmailUiState> = _uiState.asStateFlow()

    fun setEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun sendOtp(onSuccess: (String) -> Unit) {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng nhập email")
            return
        }

        if (!email.contains("@")) {
            _uiState.value = _uiState.value.copy(error = "Email không hợp lệ")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            when (val result = sendOtpUseCase(email, "FORGOT_PASSWORD")) {
                is ApiResult.Success -> onSuccess(email)
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.message.ifBlank { "Gửi OTP thất bại" }
                    )
                }
            }

            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}
