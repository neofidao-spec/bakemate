package com.bakemate.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bakemate.MainActivity
import com.bakemate.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "baking_timer"
        const val CHANNEL_NAME = "Timer Baking"
        const val NOTIFICATION_ID_STAGE_DONE = 2001
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat tahap baking selesai"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showStageDone(stageIndex: Int, stageName: String, durationMs: Long) {
        val minutes = durationMs / 60000
        val timeText = if (minutes > 0) "$minutes menit" else "${durationMs / 1000} detik"

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Tahap selesai: $stageName")
            .setContentText("Tahap berjalan $timeText. Saatnya lanjut ke tahap berikutnya!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_STAGE_DONE + stageIndex, notification)
    }

    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
