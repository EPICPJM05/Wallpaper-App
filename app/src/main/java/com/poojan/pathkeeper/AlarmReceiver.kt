package com.poojan.pathkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PrefsRepository(appContext)
                if (repo.getFolderUri() != null) {
                    Scheduler.scheduleDaily(appContext, repo.getScheduleHour(), repo.getScheduleMinute())
                    WallpaperChanger.changeNow(appContext, repo)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
