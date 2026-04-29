package com.pocketbook.data.dao

import androidx.room.*
import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun getByType(type: CategoryType): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM categories WHERE isPreset = 1")
    suspend fun getPresetCount(): Int
}
