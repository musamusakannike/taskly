package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.CommentDao
import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskLabelCrossRef

@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        LabelEntity::class,
        TaskLabelCrossRef::class,
        CommentEntity::class,
        AttachmentEntity::class,
        ReminderEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TasklyDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun labelDao(): LabelDao
    abstract fun commentDao(): CommentDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: TasklyDatabase? = null

        fun getInstance(context: Context): TasklyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TasklyDatabase::class.java,
                    "taskly.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
