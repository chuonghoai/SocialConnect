package com.example.frontend.domain.usecase.UserUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class GetMeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.getMe()
}