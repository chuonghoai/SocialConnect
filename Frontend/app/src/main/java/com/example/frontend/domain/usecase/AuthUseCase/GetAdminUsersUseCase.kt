package com.example.frontend.domain.usecase.AuthUseCase

import com.example.frontend.domain.repository.AuthRepository
import javax.inject.Inject

class GetAdminUsersUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(limit: Int = 100, offset: Int = 0) =
        repository.getAdminUsers(limit = limit, offset = offset)
}
