package com.example.frontend.domain.usecase.SearchUseCase

import com.example.frontend.domain.repository.SearchRepository
import javax.inject.Inject

class ClearSearchHistoryUseCase @Inject constructor(
    private val repo: SearchRepository
) {
    suspend operator fun invoke() = repo.clearSearchHistory()
}
