package com.bakemate.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BakeTimerEngineTest {

    private val stages = listOf(
        StageSnapshot("Autolyse", 60 * 60L),
        StageSnapshot("Bulk", 3 * 60 * 60L),
        StageSnapshot("Bake", 45 * 60L)
    )

    @Test
    fun `buildStageEnds creates cumulative ends from start`() {
        val start = 1_000_000L
        val ends = BakeTimerEngine.buildStageEnds(stages, start)

        assertEquals(3, ends.size)
        assertEquals(start + 3_600_000L, ends[0])
        assertEquals(start + 3_600_000L + 10_800_000L, ends[1])
        assertEquals(start + 3_600_000L + 10_800_000L + 2_700_000L, ends[2])
    }

    @Test
    fun `currentStageIndex returns correct stage at various times`() {
        val start = 1_000_000L
        val ends = BakeTimerEngine.buildStageEnds(stages, start)

        assertEquals(0, BakeTimerEngine.currentStageIndex(ends, start))
        assertEquals(0, BakeTimerEngine.currentStageIndex(ends, start + 3_000_000L))
        assertEquals(1, BakeTimerEngine.currentStageIndex(ends, start + 4_000_000L))
        assertEquals(2, BakeTimerEngine.currentStageIndex(ends, start + 15_000_000L))
        // Setelah semua selesai → tahap terakhir
        assertEquals(2, BakeTimerEngine.currentStageIndex(ends, start + 99_999_999L))
    }

    @Test
    fun `isFinished true only after last stage end`() {
        val start = 1_000_000L
        val ends = BakeTimerEngine.buildStageEnds(stages, start)

        assertFalse(BakeTimerEngine.isFinished(ends, start))
        assertFalse(BakeTimerEngine.isFinished(ends, start + 16_000_000L))
        assertTrue(BakeTimerEngine.isFinished(ends, start + 17_100_001L))
        assertTrue(BakeTimerEngine.isFinished(ends, start + 17_100_000L))
    }

    @Test
    fun `remainingMs is clamped at zero`() {
        val start = 1_000_000L
        val ends = BakeTimerEngine.buildStageEnds(stages, start)

        assertEquals(3_600_000L, BakeTimerEngine.remainingMs(ends, 0, start))
        assertEquals(0L, BakeTimerEngine.remainingMs(ends, 0, start + 3_600_001L))
        assertEquals(0L, BakeTimerEngine.remainingMs(ends, 5, start)) // index di luar
    }

    @Test
    fun `empty stages considered finished`() {
        assertTrue(BakeTimerEngine.isFinished(emptyList(), 123L))
        assertEquals(0, BakeTimerEngine.currentStageIndex(emptyList(), 123L))
        assertEquals(0L, BakeTimerEngine.remainingMs(emptyList(), 0, 123L))
    }

    @Test
    fun `resumeWithRemaining shifts schedule forward from current stage`() {
        val start = 1_000_000L
        val ends = BakeTimerEngine.buildStageEnds(stages, start)

        // Pause di tahap 1 (Bulk) dengan sisa 2 jam
        val now = start + 5_000_000L
        val remainingSeconds = 2 * 60 * 60L
        val resumed = BakeTimerEngine.resumeWithRemaining(ends, 1, remainingSeconds, now)

        // Tahap 1 (Bulk) berakhir: now + 2 jam
        assertEquals(now + remainingSeconds * 1000L, resumed[1])
        // Tahap 2 (Bake) berakhir: +45 menit lagi
        assertEquals(resumed[1] + 45 * 60 * 1000L, resumed[2])
        // Tahap 0 (Autolyse) tidak berubah (sudah lewat)
        assertEquals(ends[0], resumed[0])
    }
}
