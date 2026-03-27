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
import com.example.frontend.domain.usecase.MediaUseCase.UploadMediaUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadMediaUrlUseCase: UploadMediaUrlUseCase
) : ViewModel() {

    var displayName by mutableStateOf("")
    var dob by mutableStateOf("")
    var email by mutableStateOf("")

    var selectedAvatarUri by mutableStateOf<Uri?>(null)
    private var currentAvatarUrl: String? = null

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var isInitialized = false

    fun initData(user: User?) {
        if (user != null && !isInitialized) {
            displayName = user.displayName
            email = user.email
            dob = "01/01/2000" // temporary mock
            currentAvatarUrl = user.avatarUrl
            isInitialized = true
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null

            var finalAvatarToSubmit = currentAvatarUrl

            if (selectedAvatarUri != null) {
                when (val uploadRes = uploadMediaUrlUseCase(selectedAvatarUri!!)) {
                    is ApiResult.Success -> {
                        finalAvatarToSubmit = uploadRes.data
                    }
                    is ApiResult.Error -> {
                        error = "Loi tai anh len: ${uploadRes.message}"
                        isLoading = false
                        return@launch
                    }
                }
            }

            when (val result = updateProfileUseCase(displayName, dob, email, finalAvatarToSubmit)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
            }

            isLoading = false
        }
    }
}
