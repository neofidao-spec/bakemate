package com.bakemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val totalFlourGrams: Double = 0.0,
    val totalWaterGrams: Double = 0.0,
    val totalStarterGrams: Double = 0.0,
    val totalSaltGrams: Double = 0.0,
    val hydrationPercent: Double = 0.0,
    val steps: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
