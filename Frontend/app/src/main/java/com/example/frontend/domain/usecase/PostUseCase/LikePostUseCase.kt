package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class LikePostUseCase @Inject constructor(
    private val repo: PostRepository
) {
    suspend operator fun invoke(postId: String, isLiked: Boolean, likeCount: Int) =
        repo.likePost(postId, isLiked, likeCount)
}