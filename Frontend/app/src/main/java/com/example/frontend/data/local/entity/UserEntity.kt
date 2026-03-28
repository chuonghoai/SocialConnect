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
        )
    }
}

fun User.toEntity(): UserEntity {
    val safeDisplayName = (displayName as String?)?.ifBlank { (username as String?).orEmpty() }
        ?: (username as String?).orEmpty()
    val safeUsername = (username as String?).orEmpty()
    val safeEmail = (email as String?).orEmpty()
    val safePhone = (phone as String?).orEmpty()
    val safeRole = (role as String?)?.ifBlank { "CLIENT" } ?: "CLIENT"

    return UserEntity(
        id = id,
        displayName = safeDisplayName,
        username = safeUsername,
        email = safeEmail,
        phone = safePhone,
        role = safeRole,
        isOnline = isOnline,
        postCount = postCount,
        friendCount = friendCount,
        caption = caption,
        avatarUrl = avatarUrl
    )
}
