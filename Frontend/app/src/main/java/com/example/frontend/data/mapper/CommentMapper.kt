package com.example.frontend.data.mapper

import com.example.frontend.data.remote.dto.CommentResponseDto
import com.example.frontend.domain.model.Comment

fun CommentResponseDto.toDomain(): Comment = Comment(
    id = id,
    userId = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    content = content,
    createdAt = createdAt,
    likeCount = likeCount
)
