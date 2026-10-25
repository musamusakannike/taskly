package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["userId"])
    ]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val taskId: String,
    val content: String,
    val authorName: String = "You",
    val authorEmail: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["userId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val taskId: String,
    val name: String,
    val fileUri: String,
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long = 0L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["triggerTime"])
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val triggerTime: Long,
    val isFired: Boolean = false
)

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val entityType: String, // "PROJECT", "TASK", "LABEL", "COMMENT", "ATTACHMENT"
    val entityId: String,
    val operation: String, // "UPSERT", "DELETE"
    val timestamp: Long = System.currentTimeMillis()
)
