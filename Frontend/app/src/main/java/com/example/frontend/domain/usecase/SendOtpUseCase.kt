package com.example.frontend.domain.usecase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(email: String, type: String) = repo.sendOtp(email, type)
}