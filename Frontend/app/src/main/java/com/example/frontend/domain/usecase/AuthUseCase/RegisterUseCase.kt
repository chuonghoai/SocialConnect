package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, mailOtp: String) =
        repo.register(email, password, mailOtp)
}