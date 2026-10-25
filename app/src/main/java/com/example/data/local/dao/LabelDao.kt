package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllLabelsSync(): List<LabelEntity>

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun getLabelById(id: String): LabelEntity?

    @Query("SELECT * FROM labels WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getLabelByName(name: String): LabelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    @Query("UPDATE labels SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteLabel(id: String, timestamp: Long = System.currentTimeMillis())

    // Cross references
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskLabel(crossRef: TaskLabelCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskLabels(crossRefs: List<TaskLabelCrossRef>)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun clearLabelsForTask(taskId: String)

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_labels tl ON l.id = tl.labelId
        WHERE tl.taskId = :taskId AND l.isDeleted = 0
    """)
    fun getLabelsForTask(taskId: String): Flow<List<LabelEntity>>

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_labels tl ON l.id = tl.labelId
        WHERE tl.taskId = :taskId AND l.isDeleted = 0
    """)
    suspend fun getLabelsForTaskSync(taskId: String): List<LabelEntity>

    @Query("""
        SELECT t.* FROM tasks t
        INNER JOIN task_labels tl ON t.id = tl.taskId
        WHERE tl.labelId = :labelId AND t.isDeleted = 0
        ORDER BY t.isCompleted ASC, t.priority ASC, t.dueDate ASC
    """)
    fun getTasksForLabel(labelId: String): Flow<List<TaskEntity>>
}
