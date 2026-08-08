package com.bakemate.domain

data class BakeStage(
    val name: String,
    val defaultMinutes: Int,
    val isTimed: Boolean = true
)

object DefaultStages {
    val list = listOf(
        BakeStage("Autolyse", 60),
        BakeStage("Mix & Rest", 30),
        BakeStage("Bulk Fermentasi", 180),
        BakeStage("Pre-shape", 20),
        BakeStage("Final Proof", 240),
        BakeStage("Bake", 45)
    )
}
