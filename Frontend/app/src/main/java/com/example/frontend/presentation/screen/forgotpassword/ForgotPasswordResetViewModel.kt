package com.example.frontend.presentation.screen.forgotpassword

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.AuthUseCase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordResetViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val emailArg = savedStateHandle.get<String>("email")?.let(Uri::decode).orEmpty()
    private val otpArg = savedStateHandle.get<String>("otp")?.let(Uri::decode).orEmpty()

    private val _uiState = MutableStateFlow(
        ForgotPasswordResetUiState(email = emailArg, otp = otpArg)
    )
    val uiState: StateFlow<ForgotPasswordResetUiState> = _uiState.asStateFlow()

    fun setNewPassword(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value)
    }

    fun setConfirmNewPassword(value: String) {
        _uiState.value = _uiState.value.copy(confirmNewPassword = value)
    }

    fun resetPassword(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.email.isBlank() || state.otp.isBlank()) {
            _uiState.value = state.copy(error = "Thiếu thông tin xác thực")
            return
        }

        if (state.newPassword.isBlank() || state.confirmNewPassword.isBlank()) {
            _uiState.value = state.copy(error = "Vui lòng nhập mật khẩu mới và xác nhận mật khẩu")
            return
        }

        if (state.newPassword.length < 6) {
            _uiState.value = state.copy(error = "Mật khẩu mới phải có ít nhất 6 ký tự")
            return
        }

        if (state.newPassword != state.confirmNewPassword) {
            _uiState.value = state.copy(error = "Mật khẩu xác nhận không khớp")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)

            when (val result = resetPasswordUseCase(state.email, state.otp, state.newPassword)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.message.ifBlank { "Đặt lại mật khẩu thất bại" }
                    )
                }
            }

            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}
