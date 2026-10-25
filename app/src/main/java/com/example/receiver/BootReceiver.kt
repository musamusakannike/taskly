package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.TasklyDatabase
import com.example.notifications.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = TasklyDatabase.getInstance(context)
                val activeReminders = db.reminderDao().getAllActiveRemindersSync()
                val now = System.currentTimeMillis()
                for (reminder in activeReminders) {
                    if (reminder.triggerTime > now) {
                        AlarmScheduler.scheduleReminder(context, reminder.taskId, reminder.triggerTime)
                    }
                }
            }
        }
    }
}
