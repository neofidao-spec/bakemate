package com.bakemate.domain.timer

import android.os.SystemClock

/**
 * Engine timer murni berbasis elapsedRealtime — akurat walau device sleep,
 * tanpa ketergantungan loop UI. ViewModel hanya menghitung ulang dari
 * timestamp absolut.
 */
object BakeTimerEngine {

    /**
     * Buat jadwal akhir tahap (elapsedRealtime ms) dari daftar durasi.
     * [startElapsedRealtime] = SystemClock.elapsedRealtime() saat sesi dimulai.
     */
    fun buildStageEnds(
        stages: List<StageSnapshot>,
        startElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): List<Long> {
        var cursor = startElapsedRealtime
        return stages.map { stage ->
            cursor += stage.totalSeconds * 1000L
            cursor
        }
    }

    /** Index tahap aktif berdasarkan waktu sekarang. */
    fun currentStageIndex(
        stageEnds: List<Long>,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Int {
        if (stageEnds.isEmpty()) return 0
        for ((index, end) in stageEnds.withIndex()) {
            if (nowElapsedRealtime < end) return index
        }
        return stageEnds.size - 1
    }

    /** Sesi selesai? */
    fun isFinished(
        stageEnds: List<Long>,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (stageEnds.isEmpty()) return true
        return nowElapsedRealtime >= stageEnds.last()
    }

    /** Sisa waktu tahap (ms). */
    fun remainingMs(
        stageEnds: List<Long>,
        index: Int,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Long {
        val end = stageEnds.getOrNull(index) ?: return 0L
        return (end - nowElapsedRealtime).coerceAtLeast(0L)
    }

    /**
     * Restore sesi yang di-pause: geser jadwal ke depan sehingga tahap
     * saat ini punya sisa [remainingSeconds] lagi.
     */
    fun resumeWithRemaining(
        stageEnds: List<Long>,
        currentIndex: Int,
        remainingSeconds: Long,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): List<Long> {
        val result = stageEnds.toMutableList()
        var cursor = nowElapsedRealtime + remainingSeconds * 1000L
        for (i in currentIndex until result.size) {
            result[i] = cursor
            if (i + 1 < result.size) {
                // Durasi asli tahap berikutnya diambil dari nilai ORIGINAL
                // (stageEnds), bukan result yang sudah digeser.
                val originalNextDuration = stageEnds[i + 1] - stageEnds[i]
                cursor += originalNextDuration
            }
        }
        return result
    }
}
