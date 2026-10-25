package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.TasklyDatabase
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = TasklyDatabase.getInstance(context)
            val task = db.taskDao().getTaskByIdSync(taskId)
            if (task != null && !task.isCompleted && !task.isDeleted) {
                NotificationHelper.showTaskReminder(context, task)
            }
        }
    }
}
