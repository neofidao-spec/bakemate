package com.bakemate.domain.model

/**
 * Formula resep — domain model bebas Android.
 */
data class RecipeFormula(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val ingredients: List<IngredientModel> = emptyList(),
    val steps: List<StepModel> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val totalWeight: Double get() = ingredients.sumOf { it.grams }

    val flourWeight: Double get() = ingredients.filter { it.isFlour }.sumOf { it.grams }

    val waterWeight: Double get() = ingredients.filter { it.name.equals("air", true) }.sumOf { it.grams }

    val starterWeight: Double get() = ingredients.filter { it.name.equals("starter", true) }.sumOf { it.grams }

    val hydrationPercent: Double
        get() {
            val flour = flourWeight
            if (flour <= 0.0) return 0.0
            val starterFlour = starterWeight / 2.0
            val starterWater = starterWeight / 2.0
            val totalFlour = flour + starterFlour
            val totalWater = waterWeight + starterWater
            if (totalFlour <= 0.0) return 0.0
            return kotlin.math.round((totalWater / totalFlour * 100.0) * 10.0) / 10.0
        }

    val stepCount: Int get() = steps.size
}

data class IngredientModel(
    val name: String,
    val grams: Double,
    val isFlour: Boolean = false
)

data class StepModel(
    val text: String,
    val minutes: Int = 0
)
