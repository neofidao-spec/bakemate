package com.bakemate.data.repository

import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.entity.Recipe
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: RecipeDao
) {
    fun observeAll(): Flow<List<Recipe>> = dao.observeAll()

    fun observeById(id: Long): Flow<Recipe?> = dao.observeById(id)

    suspend fun getById(id: Long): Recipe? = dao.getById(id)

    suspend fun addRecipe(recipe: Recipe): Long = dao.insert(recipe)

    suspend fun updateRecipe(recipe: Recipe) = dao.update(recipe)

    suspend fun deleteRecipe(recipe: Recipe) = dao.delete(recipe)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun count(): Int = dao.count()
}
