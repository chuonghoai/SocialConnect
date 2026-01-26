package com.example.frontend.domain.usecase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String) =
        repo.login(username, password)
}
