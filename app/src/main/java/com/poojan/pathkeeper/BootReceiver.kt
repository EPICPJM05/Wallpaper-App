package com.poojan.pathkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PrefsRepository(context)
                if (repo.getFolderUri() != null) {
                    val hour = repo.getScheduleHour()
                    val minute = repo.getScheduleMinute()
                    Scheduler.scheduleDaily(context, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
