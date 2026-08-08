package com.bakemate.domain

import kotlin.math.roundToInt

object BakerMath {

    fun hydrationPercent(
        flourGram: Double,
        waterGram: Double,
        starterGram: Double = 0.0
    ): Double {
        if (flourGram <= 0.0) return 0.0
        val starterFlour = starterGram / 2.0
        val starterWater = starterGram / 2.0
        val totalFlour = flourGram + starterFlour
        val totalWater = waterGram + starterWater
        if (totalFlour <= 0.0) return 0.0
        return round1(totalWater / totalFlour * 100.0)
    }

    fun scaleToYield(
        flourGram: Double,
        waterGram: Double,
        starterGram: Double,
        saltGram: Double,
        targetYieldGram: Double
    ): List<Double>? {
        if (flourGram <= 0.0 || targetYieldGram <= 0.0) return null
        val total = flourGram + waterGram + starterGram + saltGram
        if (total <= 0.0) return null
        val factor = targetYieldGram / total
        return listOf(
            round1(flourGram * factor),
            round1(waterGram * factor),
            round1(starterGram * factor),
            round1(saltGram * factor)
        )
    }

    fun totalWeight(
        flourGram: Double,
        waterGram: Double,
        starterGram: Double,
        saltGram: Double
    ): Double = round1(flourGram + waterGram + starterGram + saltGram)

    fun flourPerDough(flourGram: Double, totalGram: Double): Double {
        if (totalGram <= 0.0) return 0.0
        return round1(flourGram / totalGram * 100.0)
    }

    private fun round1(value: Double): Double =
        (value * 10).roundToInt() / 10.0
}
