package com.bakemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesi baking yang sedang berjalan — persist agar survive app di-kill / reboot.
 */
@Entity(tableName = "bake_sessions")
data class BakeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long? = null,
    val recipeName: String,
    val stagesJson: String,          // JSON: List<StageSnapshot> (name, totalSeconds)
    val stageEndsJson: String,       // JSON: List<Long> epochMs per tahap (elapsedRealtime based)
    val currentStageIndex: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val bakeMode: Boolean = false,
    val status: String = STATUS_ACTIVE  // ACTIVE | PAUSED | DONE
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_DONE = "DONE"
    }
}
