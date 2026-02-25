package com.example.frontend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.frontend.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val username: String,
    val email: String,
    val phone: String,
    val role: String,
    val isOnline: Boolean,
    val postCount: Long,
    val friendCount: Long,
    val caption: String?,
    val avatarUrl: String?
) {
    fun toDomain(): User {
        return User(
            id = id,
            displayName = displayName,
            username = username,
            email = email,
            phone = phone,
            role = role,
            isOnline = isOnline,
            postCount = postCount,
            friendCount = friendCount,
            caption = caption,
            avatarUrl = avatarUrl,
            myPosts = emptyList()
        )
    }
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        displayName = displayName,
        username = username,
        email = email,
        phone = phone,
        role = role,
        isOnline = isOnline,
        postCount = postCount,
        friendCount = friendCount,
        caption = caption,
        avatarUrl = avatarUrl
    )
}