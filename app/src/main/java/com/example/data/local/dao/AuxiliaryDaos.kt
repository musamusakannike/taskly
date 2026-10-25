package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE taskId = :taskId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("UPDATE comments SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteComment(id: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE taskId = :taskId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAttachmentsForTask(taskId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("UPDATE attachments SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteAttachment(id: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE taskId = :taskId AND isFired = 0")
    fun getRemindersForTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isFired = 0 AND triggerTime <= :now")
    suspend fun getPendingReminders(now: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isFired = 0 ORDER BY triggerTime ASC")
    suspend fun getAllActiveRemindersSync(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isFired = 1 WHERE id = :id")
    suspend fun markReminderFired(id: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getQueueItems(limit: Int = 100): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun dequeue(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun clearEntityFromQueue(entityType: String, entityId: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingQueueCount(): Flow<Int>
}
