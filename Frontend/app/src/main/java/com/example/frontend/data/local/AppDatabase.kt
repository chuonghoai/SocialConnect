package com.example.frontend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.dao.UserDao
import com.example.frontend.data.local.entity.PostEntity
import com.example.frontend.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
}