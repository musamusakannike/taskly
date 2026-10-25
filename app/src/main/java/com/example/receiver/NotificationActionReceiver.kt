package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.TasklyDatabase
import com.example.data.local.entity.SyncQueueEntity
import com.example.notifications.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPLETE = "com.example.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, taskId.hashCode())

        // Dismiss notification
        NotificationManagerCompat.from(context).cancel(notificationId)

        when (intent.action) {
            ACTION_COMPLETE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = TasklyDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    db.taskDao().updateCompletion(taskId, true, now, now)
                    db.syncQueueDao().enqueue(
                        SyncQueueEntity(entityType = "TASK", entityId = taskId, operation = "UPSERT", timestamp = now)
                    )
                }
            }
            ACTION_SNOOZE -> {
                // Snooze 15 minutes
                val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000L
                AlarmScheduler.scheduleReminder(context, taskId, snoozeTime)
            }
        }
    }
}
