package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, createdAt DESC")
    fun getAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<Tag>)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)
}
