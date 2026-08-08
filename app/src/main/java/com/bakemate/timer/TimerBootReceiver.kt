package com.bakemate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.bakemate.data.repository.BakeSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Setelah reboot: restore sesi baking aktif dan jadwalkan ulang alarm
 * untuk tahap-tahap yang belum selesai.
 */
@AndroidEntryPoint
class TimerBootReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: BakeSessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        scope.launch {
            val session = sessionRepository.getActive() ?: return@launch
            val now = SystemClock.elapsedRealtime()

            // Lewati tahap yang sudah lewat waktunya
            val index = session.stageEnds.indexOfFirst { it > now }
                .takeIf { it >= 0 } ?: session.stageEnds.size - 1

            if (session.stageEnds.lastOrNull()?.let { now >= it } == true) {
                // Semua tahap sudah lewat → tandai selesai
                sessionRepository.markDone()
                return@launch
            }

            // Jadwalkan alarm untuk tahap aktif + sisa
            val scheduler = TimerScheduler(context)
            for (i in index until session.stageEnds.size) {
                val stage = session.stages.getOrNull(i) ?: continue
                scheduler.scheduleStage(i, stage.name, stage.totalSeconds * 1000L, session.stageEnds[i])
            }

            // Simpan index yang dikoreksi
            sessionRepository.updateSession(session.copy(currentStageIndex = index))
        }
    }
}
