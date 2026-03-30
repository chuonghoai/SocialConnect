package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(email: String, newPassword: String) =
        repo.resetPassword(email, newPassword)
}
