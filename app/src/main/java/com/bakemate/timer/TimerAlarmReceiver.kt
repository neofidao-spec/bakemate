package com.bakemate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.bakemate.data.repository.BakeSessionRepository
import com.bakemate.domain.timer.BakeTimerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Menerima alarm selesai tahap → notifikasi + update sesi di DB
 * (advance index / tandai selesai).
 */
@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: BakeSessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_STAGE_DONE = "com.bakemate.action.STAGE_DONE"
        const val ACTION_STOP_ALL = "com.bakemate.action.STOP_ALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_ALL -> {
                val count = intent.getIntExtra("stage_count", 0)
                val scheduler = TimerScheduler(context)
                scheduler.cancelAll(count)
                NotificationHelper(context).cancelAll()
                scope.launch { sessionRepository.markDone() }
            }
            else -> {
                val index = intent.getIntExtra(TimerScheduler.EXTRA_STAGE_INDEX, 0)
                val name = intent.getStringExtra(TimerScheduler.EXTRA_STAGE_NAME) ?: "Tahap"
                val durationMs = intent.getLongExtra(TimerScheduler.EXTRA_DURATION_MS, 0L)
                NotificationHelper(context).showStageDone(index, name, durationMs)

                // Update sesi: advance index & tandai selesai jika tahap terakhir
                scope.launch {
                    val session = sessionRepository.getActive() ?: return@launch
                    val now = SystemClock.elapsedRealtime()
                    if (BakeTimerEngine.isFinished(session.stageEnds, now)) {
                        sessionRepository.markDone()
                    } else {
                        val newIndex = BakeTimerEngine.currentStageIndex(session.stageEnds, now)
                        if (newIndex != session.currentStageIndex) {
                            sessionRepository.updateSession(session.copy(currentStageIndex = newIndex))
                        }
                    }
                }
            }
        }
    }
}
