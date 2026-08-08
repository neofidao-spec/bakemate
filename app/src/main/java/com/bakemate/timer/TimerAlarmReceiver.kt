package com.bakemate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {

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
            }
            else -> {
                val index = intent.getIntExtra(TimerScheduler.EXTRA_STAGE_INDEX, 0)
                val name = intent.getStringExtra(TimerScheduler.EXTRA_STAGE_NAME) ?: "Tahap"
                val durationMs = intent.getLongExtra(TimerScheduler.EXTRA_DURATION_MS, 0L)
                NotificationHelper(context).showStageDone(index, name, durationMs)
            }
        }
    }
}
