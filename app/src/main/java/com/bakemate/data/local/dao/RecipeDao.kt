package com.bakemate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.local.entity.RecipeIngredient
import com.bakemate.data.local.entity.RecipeStep
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' ORDER BY isFavorite DESC, updatedAt DESC")
    fun search(query: String): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("UPDATE recipes SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Recipe>

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFull(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: RecipeIngredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: RecipeStep): Long

    // ===== Ingredients =====
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getIngredients(recipeId: Long): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsFor(recipeId: Long)

    // ===== Steps =====
    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun observeSteps(recipeId: Long): Flow<List<RecipeStep>>

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getSteps(recipeId: Long): List<RecipeStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RecipeStep>)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsFor(recipeId: Long)

    /** Simpan resep lengkap (header + bahan + langkah) dalam satu transaksi. */
    @Transaction
    suspend fun saveFullRecipe(
        recipe: Recipe,
        ingredients: List<RecipeIngredient>,
        steps: List<RecipeStep>
    ): Long {
        val id = if (recipe.id == 0L) insert(recipe) else {
            update(recipe)
            recipe.id
        }
        deleteIngredientsFor(id)
        deleteStepsFor(id)
        insertIngredients(ingredients.map { it.copy(recipeId = id) })
        insertSteps(steps.map { it.copy(recipeId = id) })
        return id
    }

    /** Hapus resep lengkap (cascade via FK). */
    @Transaction
    suspend fun deleteFullRecipe(recipe: Recipe) {
        delete(recipe)
    }
}
