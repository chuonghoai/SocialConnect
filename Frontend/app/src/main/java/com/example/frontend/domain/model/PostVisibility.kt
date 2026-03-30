package com.example.frontend.domain.model

import java.text.Normalizer

object PostVisibility {
    const val PUBLIC = "PUBLIC"
    const val FRIENDS = "FRIENDS"
    const val PRIVATE = "PRIVATE"

    val options: List<String> = listOf(PUBLIC, FRIENDS, PRIVATE)

    fun normalize(raw: String?): String {
        val source = raw.orEmpty().trim()
        if (source.isBlank()) return PUBLIC

        val normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            normalized.contains("friend") || normalized.contains("ban be") -> FRIENDS
            normalized.contains("private") || normalized.contains("rieng tu") || normalized.contains("only me") -> PRIVATE
            normalized.contains("public") || normalized.contains("cong khai") -> PUBLIC
            else -> PUBLIC
        }
    }

    fun label(value: String?): String {
        return when (normalize(value)) {
            FRIENDS -> "Bạn bè"
            PRIVATE -> "Riêng tư"
            else -> "Công khai"
        }
    }
}

fun String?.normalizeVisibility(): String = PostVisibility.normalize(this)
