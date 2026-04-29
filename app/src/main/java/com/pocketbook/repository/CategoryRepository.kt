package com.pocketbook.repository

import com.pocketbook.data.dao.CategoryDao
import com.pocketbook.data.entity.Category
import com.pocketbook.data.entity.CategoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll()

    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = categoryDao.getByType(type)

    suspend fun getCategoryById(id: String): Category? = categoryDao.getById(id)

    suspend fun createCategory(category: Category) = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun getPresetCount(): Int = categoryDao.getPresetCount()
}
