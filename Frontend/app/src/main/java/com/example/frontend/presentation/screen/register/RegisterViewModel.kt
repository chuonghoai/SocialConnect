package com.example.frontend.presentation.screen.register

import android.util.Patterns
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

    fun setEmail(v: String) {
        _uiState.value = _uiState.value.copy(email = v)
    }

    fun setPassword(v: String) {
        _uiState.value = _uiState.value.copy(password = v)
    }

    fun setConfirmPassword(v: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = v)
    }

    fun updateOtp(v: String) {
        _uiState.value = _uiState.value.copy(otp = v, error = null)
    }

    fun sendOtp(onSendOtpClick: () -> Unit) {
        val state = _uiState.value
        val normalizedEmail = state.email.trim()

        if (normalizedEmail.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.value = state.copy(
                loading = false,
                error = "Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 th\u00f4ng tin"
            )
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            _uiState.value = state.copy(
                loading = false,
                error = "Email kh\u00f4ng \u0111\u00fang \u0111\u1ecbnh d\u1ea1ng"
            )
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(
                loading = false,
                error = "M\u1eadt kh\u1ea9u kh\u00f4ng kh\u1edbp"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val res = sendOtpUseCase(normalizedEmail, "REGISTER")) {
                is ApiResult.Success -> onSendOtpClick()
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = mapSendOtpError(res)
                    )
                }
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun register(onRegisterClick: () -> Unit) {
        val state = _uiState.value

        if (state.otp.length != 6) {
            _uiState.value = state.copy(
                loading = false,
                error = "Vui lòng nhập đủ 6 số OTP"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            when (val res = registerUseCase(state.email, state.password, state.otp)) {
                is ApiResult.Success -> onRegisterClick()
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = mapRegisterError(res)
                    )
                }
            }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    private fun mapSendOtpError(error: ApiResult.Error): String {
        val raw = error.message.trim()
        val lower = raw.lowercase()

        return when {
            error.code == 409 -> "Email \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng"
            error.code == 429 || lower.contains("too many") || lower.contains("limit exceeded") ->
                "B\u1ea1n y\u00eau c\u1ea7u OTP qu\u00e1 nhi\u1ec1u l\u1ea7n, vui l\u00f2ng th\u1eed l\u1ea1i sau"
            lower.contains("email") && (lower.contains("invalid") || lower.contains("format")) ->
                "Email kh\u00f4ng h\u1ee3p l\u1ec7"
            lower.contains("network") || lower.contains("timeout") || lower.contains("k\u1ebft n\u1ed1i") ->
                "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i m\u00e1y ch\u1ee7, vui l\u00f2ng ki\u1ec3m tra m\u1ea1ng"
            raw.isBlank() || lower.contains("http") ->
                "Kh\u00f4ng th\u1ec3 g\u1eedi m\u00e3 x\u00e1c th\u1ef1c, vui l\u00f2ng th\u1eed l\u1ea1i"
            else -> "Kh\u00f4ng th\u1ec3 g\u1eedi m\u00e3 x\u00e1c th\u1ef1c, vui l\u00f2ng th\u1eed l\u1ea1i"
        }
    }

    private fun mapRegisterError(error: ApiResult.Error): String {
        val raw = error.message.trim()
        val lower = raw.lowercase()

        return when {
            lower.contains("invalid otp") || lower.contains("otp kh\u00f4ng") ->
                "M\u00e3 OTP kh\u00f4ng \u0111\u00fang"
            lower.contains("otp expired") || lower.contains("h\u1ebft h\u1ea1n") ->
                "M\u00e3 OTP \u0111\u00e3 h\u1ebft h\u1ea1n, vui l\u00f2ng y\u00eau c\u1ea7u m\u00e3 m\u1edbi"
            lower.contains("blocked") ->
                "T\u00e0i kho\u1ea3n t\u1ea1m b\u1ecb kh\u00f3a do nh\u1eadp sai OTP qu\u00e1 nhi\u1ec1u l\u1ea7n"
            lower.contains("email") && lower.contains("exist") ->
                "Email kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c \u0111\u00e3 t\u1ed3n t\u1ea1i"
            lower.contains("network") || lower.contains("timeout") || lower.contains("k\u1ebft n\u1ed1i") ->
                "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i m\u00e1y ch\u1ee7, vui l\u00f2ng ki\u1ec3m tra m\u1ea1ng"
            raw.isBlank() || lower.contains("http") ->
                "\u0110\u0103ng k\u00fd th\u1ea5t b\u1ea1i, vui l\u00f2ng th\u1eed l\u1ea1i"
            else -> "\u0110\u0103ng k\u00fd th\u1ea5t b\u1ea1i, vui l\u00f2ng th\u1eed l\u1ea1i"
        }
    }
}
