package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newPassword: String, confirmPassword: String): ApiResult<Unit> {
        return authRepository.changePassword(newPassword, confirmPassword)
    }
}
