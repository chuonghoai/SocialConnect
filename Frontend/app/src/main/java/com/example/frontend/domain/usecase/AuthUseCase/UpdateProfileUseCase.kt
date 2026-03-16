package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(displayName: String, dob: String, phone: String, avatar: String?): ApiResult<User> {
        return authRepository.updateProfile(displayName, dob, phone, avatar)
    }
}