package com.example.frontend.presentation.screen.setting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.usecase.AuthUseCase.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun submit(onSuccess: () -> Unit) {
        val newValue = newPassword.trim()
        val confirmValue = confirmPassword.trim()

        if (newValue.isBlank() || confirmValue.isBlank()) {
            error = "Vui long nhap day du mat khau moi"
            return
        }

        if (newValue.length < 6) {
            error = "Mat khau moi phai co it nhat 6 ky tu"
            return
        }

        if (newValue != confirmValue) {
            error = "Mat khau xac nhan khong khop"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            when (val result = changePasswordUseCase(newValue, confirmValue)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> {
                    error = result.message.ifBlank { "Doi mat khau that bai" }
                }
            }

            isLoading = false
        }
    }
}
