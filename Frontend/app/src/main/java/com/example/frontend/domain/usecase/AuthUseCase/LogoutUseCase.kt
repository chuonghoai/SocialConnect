package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke() = repo.logout()
}