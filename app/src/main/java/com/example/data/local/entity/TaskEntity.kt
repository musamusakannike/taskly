package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["projectId"]),
        Index(value = ["parentTaskId"]),
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val projectId: String,
    val parentTaskId: String? = null,
    val title: String,
    val description: String = "",
    val priority: Int = 4, // 1 = P1 Urgent, 2 = P2 High, 3 = P3 Medium, 4 = P4 Normal
    val dueDate: Long? = null, // Epoch millis at start of day
    val dueTimeMinutes: Int? = null, // Minutes from midnight (e.g. 15*60 for 3:00 PM)
    val isAllDay: Boolean = true,
    val recurrenceRule: String? = null, // e.g. "DAILY", "WEEKLY", "MONTHLY", "CUSTOM:..."
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val estimatedMinutes: Int? = null,
    val orderIndex: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
