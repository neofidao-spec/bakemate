package com.bakemate.domain.timer

/**
 * Snapshot tahap baking — pure Kotlin, siap serialisasi JSON.
 */
data class StageSnapshot(
    val name: String,
    val totalSeconds: Long
)

/**
 * Sesi baking — pure Kotlin domain model.
 * Waktu berbasis [elapsedRealtimeBaseMs]: nilai dari SystemClock.elapsedRealtime()
 * saat sesi dimulai. Dengan ini hitung mundur akurat walau device sleep
 * (elapsedRealtime terus berjalan).
 */
data class BakeSessionModel(
    val id: Long = 0,
    val recipeId: Long? = null,
    val recipeName: String,
    val stages: List<StageSnapshot>,
    val currentStageIndex: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val bakeMode: Boolean = false,
    val status: String = STATUS_ACTIVE,
    /** Waktu selesai absolut (elapsedRealtime) untuk tiap tahap. */
    val stageEnds: List<Long>
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_DONE = "DONE"
    }

    /** Waktu tersisa tahap saat ini, dalam detik. */
    fun remainingSeconds(nowElapsedRealtime: Long): Long {
        val end = stageEnds.getOrNull(currentStageIndex) ?: return 0L
        return ((end - nowElapsedRealtime) / 1000L).coerceAtLeast(0L)
    }

    /** Total durasi semua tahap, detik. */
    fun totalDurationSeconds(): Long = stages.sumOf { it.totalSeconds }

    /** Progress keseluruhan 0f..1f berdasarkan waktu. */
    fun overallProgress(nowElapsedRealtime: Long): Float {
        val total = totalDurationSeconds().coerceAtLeast(1L)
        val elapsed = total - (stageEnds.lastOrNull()?.let { (it - nowElapsedRealtime) / 1000L } ?: 0L)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
