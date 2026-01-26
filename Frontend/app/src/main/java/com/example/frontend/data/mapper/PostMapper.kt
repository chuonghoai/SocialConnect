package com.example.frontend.data.mapper

import com.example.frontend.data.remote.dto.PostResponseDto
import com.example.frontend.domain.model.Post

fun PostResponseDto.toDomain(): Post = Post(
    id = id,
    userId = userId,
    displayName = displayName,
    userAvatar = userAvatar,
    content = content,
    type = type,
    kind = kind,
    createdAt = createdAt,
    likeCount = likeCount,
    commentCount = commentCount,
    shareCount = shareCount,
    cdnUrl = cdnUrl
)
