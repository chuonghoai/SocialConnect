package com.example.frontend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.frontend.data.local.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPostsByUserId(userId: String): List<PostEntity>

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun clearUserPosts(userId: String)

    @Query("SELECT * FROM posts WHERE kind = 'VIDEO' ORDER BY createdAt DESC")
    suspend fun getCachedVideos(): List<PostEntity>

    @Query("DELETE FROM posts WHERE kind = 'VIDEO'")
    suspend fun clearCachedVideos()
}