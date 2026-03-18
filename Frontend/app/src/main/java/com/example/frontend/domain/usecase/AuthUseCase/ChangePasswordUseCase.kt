package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.AuthRepository
import jakarta.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(oldPassword: String, newPassword: String): ApiResult<Unit> {
        return authRepository.changePassword(oldPassword, newPassword)
    }
}