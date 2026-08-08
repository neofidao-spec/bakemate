package com.bakemate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    }
}
