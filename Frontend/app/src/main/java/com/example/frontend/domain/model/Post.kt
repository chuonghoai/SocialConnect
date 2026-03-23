package com.example.frontend.domain.model

import com.google.gson.annotations.SerializedName

data class Post(
    val id: String,
    val userId: String,
    val displayName: String,
    val userAvatar: String,
    val content: String,
    val type: String,
    val kind: String,
    val createdAt: String,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val cdnUrl: String,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val originalPost: OriginalPost? = null,
    @SerializedName(
        value = "media",
        alternate = ["medias", "attachments", "files", "mediaList"]
    )
    val media: List<PostMedia>? = null,
    @SerializedName(
        value = "mediaId",
        alternate = ["mediaIds", "mediaID", "mediaIDs"]
    )
    val mediaIds: List<PostMedia>? = null,
    @SerializedName(
        value = "cdnUrls",
        alternate = ["urls", "mediaUrls", "imageUrls", "videoUrls"]
    )
    val mediaUrls: List<String>? = null,
    @SerializedName(value = "images")
    val images: List<String>? = null,
    @SerializedName(value = "videos")
    val videos: List<String>? = null
)

data class PostMedia(
    @SerializedName(
        value = "cdnUrl",
        alternate = [
            "url",
            "secure_url",
            "secureUrl",
            "cdn_url",
            "link",
            "path",
            "mediaUrl",
            "media_url",
            "imageUrl",
            "videoUrl",
            "mediaId"
        ]
    )
    val cdnUrl: String? = null,
    @SerializedName(
        value = "kind",
        alternate = ["type", "resource_type", "resourceType", "mediaType"]
    )
    val kind: String? = null,
    @SerializedName(value = "media", alternate = ["file", "attachment", "asset"])
    val nested: NestedMedia? = null
) {
    fun resolvedUrl(): String {
        return when {
            !cdnUrl.isNullOrBlank() -> cdnUrl.orEmpty()
            !nested?.url.isNullOrBlank() -> nested?.url.orEmpty()
            !nested?.cdnUrl.isNullOrBlank() -> nested?.cdnUrl.orEmpty()
            else -> ""
        }
    }
}

data class NestedMedia(
    @SerializedName(value = "url", alternate = ["secure_url", "secureUrl", "link"])
    val url: String = "",
    @SerializedName(value = "cdnUrl", alternate = ["cdn_url", "path"])
    val cdnUrl: String = ""
)

data class OriginalPost(
    val id: String,
    val userId: String,
    val displayName: String,
    val userAvatar: String,
    val content: String,
    val kind: String,
    val cdnUrl: String,
    val createdAt: String
)
