package com.bakemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starter_logs")
data class StarterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val starterName: String = "Starter Utama",
    val feedingTime: Long = System.currentTimeMillis(),
    val ratio: String = "",
    val note: String = "",
    val activityLevel: Int = 3,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
