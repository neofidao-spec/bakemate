package com.bakemate.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bakemate.permission.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_STAGE_INDEX = "stage_index"
        const val EXTRA_STAGE_NAME = "stage_name"
        const val EXTRA_DURATION_MS = "duration_ms"
    }

    fun scheduleStage(stageIndex: Int, stageName: String, durationMs: Long, triggerAtMillis: Long) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_STAGE_DONE
            putExtra(EXTRA_STAGE_INDEX, stageIndex)
            putExtra(EXTRA_STAGE_NAME, stageName)
            putExtra(EXTRA_DURATION_MS, durationMs)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stageIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (PermissionHelper.hasExactAlarmPermission(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            // Fallback: alarm inexact (tetap bekerja, mungkin telat beberapa menit)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelStage(stageIndex: Int) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_STAGE_DONE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stageIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(stageCount: Int) {
        for (i in 0 until stageCount) {
            cancelStage(i)
        }
    }
}
