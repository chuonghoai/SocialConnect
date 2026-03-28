package com.example.frontend.domain.usecase.PostUseCase
import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class GetUserPostsUseCase @Inject constructor(private val repo: PostRepository) {
    suspend operator fun invoke(
        userId: String,
        afterId: String? = null,
        isRefresh: Boolean = false,
        includeHidden: Boolean = false
    ) = repo.getUserPosts(userId, afterId, isRefresh, includeHidden)
}
