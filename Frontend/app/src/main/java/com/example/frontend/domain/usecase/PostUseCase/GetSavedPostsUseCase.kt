package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class GetSavedPostsUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(afterId: String? = null, isRefresh: Boolean = false) =
        repo.getSavedPosts(afterId, isRefresh)
}
