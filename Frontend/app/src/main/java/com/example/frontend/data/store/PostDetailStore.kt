package com.example.frontend.data.store

import com.example.frontend.domain.model.Post
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store dùng để truyền Post object giữa HomeScreen → PostDetailScreen
 * mà không cần serialize qua nav args.
 */
@Singleton
class PostDetailStore @Inject constructor() {
    var selectedPost: Post? = null
}
