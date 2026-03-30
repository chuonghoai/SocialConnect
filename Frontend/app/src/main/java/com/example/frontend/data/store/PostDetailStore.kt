package com.example.frontend.data.store

import com.example.frontend.domain.model.Post
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostDetailStore @Inject constructor() {
    var selectedPost: Post? = null
}
