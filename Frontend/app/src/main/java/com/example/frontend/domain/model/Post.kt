package com.example.frontend.domain.model

import com.google.gson.annotations.SerializedName

data class PostMedia(
    @SerializedName(value = "publicId", alternate = ["public_id", "id", "mediaPublicId"])
    val publicId: String? = null,
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
            "videoUrl"
        ]
    )
    val cdnUrl: String = "",
    @SerializedName(
        value = "kind",
        alternate = ["type", "resource_type", "resourceType", "mediaType"]
    )
    val kind: String = "",
    @SerializedName(value = "media", alternate = ["file", "attachment", "asset"])
    val nested: NestedMedia? = null
) {
    fun resolvedUrl(): String {
        return when {
            cdnUrl.isNotBlank() -> cdnUrl
            nested?.url?.isNotBlank() == true -> nested.url
            nested?.cdnUrl?.isNotBlank() == true -> nested.cdnUrl
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
    val isHiddenByAdmin: Boolean = false,
    @SerializedName(
        value = "media",
        alternate = ["medias", "attachments", "files", "mediaList"]
    )
    val media: List<PostMedia>? = emptyList(),
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val originalPost: OriginalPost? = null,
    @SerializedName(
        value = "mediaIds_alt",
        alternate = ["mediaIds", "mediaID", "mediaIDs", "mediaId"]
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
