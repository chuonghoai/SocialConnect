package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class ReportPostUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(postId: String, reason: String) = repo.reportPost(postId, reason)
}
