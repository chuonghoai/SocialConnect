package com.example.frontend.domain.usecase.MediaUseCase

import android.net.Uri
import com.example.frontend.domain.repository.MediaRepository
import javax.inject.Inject

class UploadMediaUrlUseCase @Inject constructor(
    private val repo: MediaRepository
) {
    suspend operator fun invoke(uri: Uri) = repo.uploadMediaUrl(uri)
}
