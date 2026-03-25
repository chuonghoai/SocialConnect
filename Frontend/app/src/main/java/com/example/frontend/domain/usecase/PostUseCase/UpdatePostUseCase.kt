package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class UpdatePostUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(
        postId: String,
        content: String? = null,
        visibility: String? = null
    ) = repo.updatePost(postId, content, visibility)
}
