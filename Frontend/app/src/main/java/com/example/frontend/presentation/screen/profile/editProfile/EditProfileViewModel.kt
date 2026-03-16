package com.example.frontend.presentation.screen.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.AuthUseCase.UpdateProfileUseCase
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase
) : ViewModel() {

    var displayName by mutableStateOf("")
    var dob by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")

    // Lưu tạm Uri của ảnh người dùng vừa chọn từ thư viện
    var selectedAvatarUri by mutableStateOf<Uri?>(null)
    private var currentAvatarUrl: String? = null

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var isInitialized = false

    fun initData(user: User?) {
        if (user != null && !isInitialized) {
            displayName = user.displayName
            email = user.email
            phone = user.phone ?: ""
            dob = "01/01/2000" // Mock tạm
            currentAvatarUrl = user.avatarUrl
            isInitialized = true
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null

            var finalAvatarToSubmit = currentAvatarUrl

            // 1. Nếu có chọn ảnh mới -> Gọi upload ảnh trước
            if (selectedAvatarUri != null) {
                when (val uploadRes = uploadMediaUseCase(selectedAvatarUri!!)) {
                    is ApiResult.Success -> {
                        finalAvatarToSubmit = uploadRes.data // Gắn url/id mới
                    }
                    is ApiResult.Error -> {
                        error = "Lỗi tải ảnh lên: ${uploadRes.message}"
                        isLoading = false
                        return@launch
                    }
                }
            }

            // 2. Gọi API cập nhật thông tin
            when (val result = updateProfileUseCase(displayName, dob, phone, finalAvatarToSubmit)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
            }

            isLoading = false
        }
    }
}