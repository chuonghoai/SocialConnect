package com.example.frontend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lưu lịch sử tìm kiếm vào Room.
 * PrimaryKey = keyword → mỗi keyword chỉ có 1 bản ghi;
 * tìm lại cùng keyword chỉ cập nhật timestamp (REPLACE strategy).
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis()
)
