package com.example.frontend.domain.model

import com.google.gson.annotations.SerializedName

data class SearchUserItem(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val isFriend: Boolean,
    @SerializedName(
        value = "friendshipStatus",
        alternate = ["friendship_status", "relationStatus", "relation_status", "status"]
    )
    val friendshipStatus: String? = "NONE"
)
