package com.bakemate.domain.model

import com.bakemate.domain.usecase.ValidateRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeFormulaTest {

    private fun formula(
        name: String = "Sourdough Dasar",
        ingredients: List<IngredientModel> = listOf(
            IngredientModel("Tepung", 500.0, isFlour = true),
            IngredientModel("Air", 350.0),
            IngredientModel("Starter", 100.0),
            IngredientModel("Garam", 10.0)
        ),
        steps: List<StepModel> = listOf(StepModel("Uleni", 30))
    ) = RecipeFormula(name = name, ingredients = ingredients, steps = steps)

    @Test
    fun `hydration includes starter 50-50`() {
        // flour 500 + starter 100/2 = 550 total flour
        // water 350 + starter 100/2 = 400 total water
        // hydration = 400/550 = 72.7%
        val f = formula()
        assertEquals(72.7, f.hydrationPercent, 0.1)
    }

    @Test
    fun `hydration zero when no flour`() {
        val f = formula(ingredients = listOf(IngredientModel("Air", 300.0)))
        assertEquals(0.0, f.hydrationPercent, 0.0)
    }

    @Test
    fun `totalWeight sums all ingredients`() {
        val f = formula()
        assertEquals(960.0, f.totalWeight, 0.0)
    }

    @Test
    fun `flourWeight only counts isFlour`() {
        val f = formula()
        assertEquals(500.0, f.flourWeight, 0.0)
    }

    @Test
    fun `validate passes for valid recipe`() {
        assertNull(ValidateRecipe.validate(formula()))
    }

    @Test
    fun `validate rejects blank name`() {
        val error = ValidateRecipe.validate(formula(name = "  "))
        assertNotNull(error)
        assertTrue(error!!.contains("Nama"))
    }

    @Test
    fun `validate rejects empty ingredients`() {
        val error = ValidateRecipe.validate(formula(ingredients = emptyList()))
        assertNotNull(error)
        assertTrue(error!!.contains("bahan"))
    }

    @Test
    fun `validate rejects no flour`() {
        val error = ValidateRecipe.validate(
            formula(ingredients = listOf(IngredientModel("Air", 300.0)))
        )
        assertNotNull(error)
        assertTrue(error!!.contains("tepung"))
    }

    @Test
    fun `validate rejects zero grams`() {
        val error = ValidateRecipe.validate(
            formula(ingredients = listOf(
                IngredientModel("Tepung", 500.0, isFlour = true),
                IngredientModel("Air", 0.0)
            ))
        )
        assertNotNull(error)
        assertTrue(error!!.contains("gram"))
    }

    @Test
    fun `validate rejects empty steps`() {
        val error = ValidateRecipe.validate(formula(steps = emptyList()))
        assertNotNull(error)
        assertTrue(error!!.contains("langkah"))
    }
}
