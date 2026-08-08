package com.bakemate.data.repository

import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.local.entity.RecipeIngredient
import com.bakemate.data.local.entity.RecipeStep
import com.bakemate.domain.model.IngredientModel
import com.bakemate.domain.model.RecipeFormula
import com.bakemate.domain.model.StepModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: RecipeDao
) {

    /** Observasi daftar formula (header saja). */
    fun observeAll(): Flow<List<RecipeFormula>> = dao.observeAll().map { recipes ->
        recipes.map { it.toFormulaShort() }
    }

    private fun Recipe.toFormulaShort(): RecipeFormula = RecipeFormula(
        id = id,
        name = name,
        description = description,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /** Observasi satu formula lengkap (header + bahan + langkah). */
    fun observeById(id: Long): Flow<RecipeFormula?> {
        val header = dao.observeById(id)
        val ingredients = dao.observeIngredients(id)
        val steps = dao.observeSteps(id)
        return combine(header, ingredients, steps) { recipe, ing, st ->
            recipe?.let {
                RecipeFormula(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    ingredients = ing.sortedBy { i -> i.sortOrder }.map { i ->
                        IngredientModel(name = i.name, grams = i.grams, isFlour = i.isFlour)
                    },
                    steps = st.sortedBy { s -> s.sortOrder }.map { s ->
                        StepModel(text = s.text, minutes = s.minutes)
                    },
                    isFavorite = it.isFavorite,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    suspend fun getById(id: Long): RecipeFormula? {
        val recipe = dao.getById(id) ?: return null
        return RecipeFormula(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            ingredients = dao.getIngredients(id).sortedBy { it.sortOrder }.map {
                IngredientModel(name = it.name, grams = it.grams, isFlour = it.isFlour)
            },
            steps = dao.getSteps(id).sortedBy { it.sortOrder }.map {
                StepModel(text = it.text, minutes = it.minutes)
            },
            isFavorite = recipe.isFavorite,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt
        )
    }

    fun search(query: String): Flow<List<RecipeFormula>> = dao.search(query)

    /** Simpan formula lengkap. */
    suspend fun saveFormula(formula: RecipeFormula): Long {
        val now = System.currentTimeMillis()
        val recipe = Recipe(
            id = formula.id,
            name = formula.name,
            description = formula.description,
            hydrationPercent = formula.hydrationPercent,
            isFavorite = formula.isFavorite,
            createdAt = if (formula.createdAt == 0L) now else formula.createdAt,
            updatedAt = now
        )
        val ingredients = formula.ingredients.mapIndexed { index, ing ->
            RecipeIngredient(
                recipeId = formula.id,
                name = ing.name,
                grams = ing.grams,
                isFlour = ing.isFlour,
                sortOrder = index
            )
        }
        val steps = formula.steps.mapIndexed { index, step ->
            RecipeStep(
                recipeId = formula.id,
                text = step.text,
                minutes = step.minutes,
                sortOrder = index
            )
        }
        return dao.saveFullRecipe(recipe, ingredients, steps)
    }

    suspend fun deleteFormula(id: Long) {
        dao.getById(id)?.let { dao.deleteFullRecipe(it) }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun count(): Int = dao.count()
}
