package com.example.frontend.domain.usecase.SearchUseCase

import com.example.frontend.domain.repository.SearchRepository
import javax.inject.Inject

class DeleteSearchHistoryUseCase @Inject constructor(
    private val repo: SearchRepository
) {
    suspend operator fun invoke(keyword: String) = repo.deleteSearchHistory(keyword)
}
