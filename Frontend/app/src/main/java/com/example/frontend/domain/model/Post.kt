package com.example.frontend.domain.model

import com.google.gson.annotations.SerializedName

data class PostMedia(
    val kind: String = "",
    val cdnUrl: String = ""
)

data class OriginalPost(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val kind: String = "",
    val cdnUrl: String = "",
    val createdAt: String = "",
    @SerializedName(value = "media", alternate = ["medias", "mediaList"])
    val media: List<PostMedia> = emptyList()
)

data class Post(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val visibility: String = "Công khai",
    val type: String = "",
    val kind: String = "",
    val createdAt: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val cdnUrl: String = "",
    @SerializedName(value = "media", alternate = ["medias", "mediaList"])
    val media: List<PostMedia> = emptyList(),
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val originalPost: OriginalPost? = null
)

