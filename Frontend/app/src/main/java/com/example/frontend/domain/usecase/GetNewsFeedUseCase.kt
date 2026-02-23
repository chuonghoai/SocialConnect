package com.example.frontend.domain.usecase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class GetNewsFeedUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(afterId: String? = null) = repo.getNewsFeed(afterId)
}