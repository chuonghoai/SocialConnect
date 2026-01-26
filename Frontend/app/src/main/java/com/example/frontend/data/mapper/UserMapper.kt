package com.example.frontend.data.mapper

import com.example.frontend.data.remote.dto.MeResponseDto
import com.example.frontend.domain.model.User

fun MeResponseDto.toDomain(): User = User(
    displayName = displayName,
    username = username,
    avatarUrl = avatarUrl
)
