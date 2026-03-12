package com.example.frontend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.frontend.data.local.entity.SearchHistoryEntity

@Dao
interface SearchHistoryDao {

    /** Lấy tối đa 20 mục gần nhất, sắp xếp theo thời gian mới nhất trước. */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    suspend fun getAll(): List<SearchHistoryEntity>

    /** Thêm mới hoặc cập nhật timestamp nếu keyword đã tồn tại. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE keyword = :keyword")
    suspend fun deleteByKeyword(keyword: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
