package com.example.frontend.domain.usecase.UserUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.User
import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String): ApiResult<User> {
        return authRepository.getUserProfile(userId)
    }
}
