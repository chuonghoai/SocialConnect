package com.example.frontend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.entity.PostEntity

@Database(
    entities = [PostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}