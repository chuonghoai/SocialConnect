package com.example.frontend.presentation.screen.forgotpassword

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.AuthUseCase.SendOtpUseCase
import com.example.frontend.domain.usecase.AuthUseCase.VerifyForgotPasswordOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordOtpViewModel @Inject constructor(
    private val verifyForgotPasswordOtpUseCase: VerifyForgotPasswordOtpUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val RESEND_TIMEOUT_SECONDS = 300
    }

    private val emailArg = savedStateHandle.get<String>("email")?.let(Uri::decode).orEmpty()
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(
        ForgotPasswordOtpUiState(
            email = emailArg,
            canResend = false,
            secondsUntilResend = RESEND_TIMEOUT_SECONDS
        )
    )
    val uiState: StateFlow<ForgotPasswordOtpUiState> = _uiState.asStateFlow()

    init {
        startResendCountdown(RESEND_TIMEOUT_SECONDS)
    }

    fun updateOtp(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(otp = value)
        }
    }

    fun verifyOtp(onSuccess: (String, String) -> Unit) {
        val state = _uiState.value
        val otp = state.otp
        val email = state.email

        if (email.isBlank()) {
            _uiState.value = state.copy(error = "Thiếu email")
            return
        }

        if (otp.length != 6) {
            _uiState.value = state.copy(error = "OTP phải gồm 6 chữ số")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)

            when (val result = verifyForgotPasswordOtpUseCase(email, otp)) {
                is ApiResult.Success -> onSuccess(email, otp)
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = mapOtpErrorMessage(result)
                    )
                }
            }

            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun resendOtp(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.canResend || state.email.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)

            when (val result = sendOtpUseCase(state.email, "FORGOT_PASSWORD")) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false)
                    startResendCountdown(RESEND_TIMEOUT_SECONDS)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.message.ifBlank { "Gửi lại OTP thất bại" }
                    )
                }
            }
        }
    }

    private fun mapOtpErrorMessage(error: ApiResult.Error): String {
        val raw = error.message.lowercase()
        val isOtpError =
            error.code == 400 ||
                error.code == 401 ||
                raw.contains("otp") ||
                raw.contains("invalid") ||
                raw.contains("expired") ||
                raw.contains("hết hạn") ||
                raw.contains("không hợp lệ")

        return if (isOtpError) {
            "Mã OTP không hợp lệ hoặc đã hết hạn"
        } else {
            error.message.ifBlank { "Xác thực OTP thất bại" }
        }
    }

    private fun startResendCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                canResend = false,
                secondsUntilResend = seconds
            )

            for (remaining in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(secondsUntilResend = remaining)
                delay(1000)
            }

            _uiState.value = _uiState.value.copy(canResend = true, secondsUntilResend = 0)
        }
    }
}
