package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class HidePostByAdminUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String) = postRepository.hidePostByAdmin(postId)
}
