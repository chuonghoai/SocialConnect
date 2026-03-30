package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class GetNewsFeedUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(
        afterId: String? = null,
        isRefresh: Boolean = false,
        includeHidden: Boolean = false
    ) = repo.getNewsFeed(afterId, isRefresh, includeHidden)
}
