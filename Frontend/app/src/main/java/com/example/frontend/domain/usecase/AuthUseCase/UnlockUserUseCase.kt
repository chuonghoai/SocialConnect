package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class UnlockUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String) = authRepository.unlockUser(userId)
}
