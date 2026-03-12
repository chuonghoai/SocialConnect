package com.example.frontend.domain.usecase.PostUseCase

import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.repository.PostRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(private val repo: PostRepository) {
    suspend operator fun invoke(content: String, visibility: String, mediaId: String?): ApiResult<String> =
        repo.createPost(content, visibility, mediaId)
}