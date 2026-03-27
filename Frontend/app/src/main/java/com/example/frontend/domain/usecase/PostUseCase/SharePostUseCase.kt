package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class SharePostUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(
        postId: String,
        content: String? = null,
        visibility: String? = null
    ) = repo.sharePost(postId, content, visibility)
}
