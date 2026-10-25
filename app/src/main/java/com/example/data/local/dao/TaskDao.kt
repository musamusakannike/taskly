package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, priority ASC, dueDate ASC, orderIndex ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isDeleted = 0 ORDER BY isCompleted ASC, priority ASC, orderIndex ASC, createdAt ASC")
    fun getTasksForProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND parentTaskId IS NULL AND isDeleted = 0 ORDER BY isCompleted ASC, priority ASC, orderIndex ASC, createdAt ASC")
    fun getRootTasksForProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND isDeleted = 0 ORDER BY isCompleted ASC, priority ASC, dueTimeMinutes ASC")
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate < :startOfToday AND isCompleted = 0 AND isDeleted = 0 ORDER BY priority ASC, dueDate ASC")
    fun getOverdueTasks(startOfToday: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfToday AND dueDate <= :endOfWeek AND isDeleted = 0 ORDER BY dueDate ASC, priority ASC")
    fun getUpcomingTasks(startOfToday: Long, endOfWeek: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY completedAt DESC LIMIT 200")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId AND isDeleted = 0 ORDER BY isCompleted ASC, orderIndex ASC, createdAt ASC")
    fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId AND isDeleted = 0 ORDER BY isCompleted ASC, orderIndex ASC, createdAt ASC")
    suspend fun getSubtasksSync(parentTaskId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0 LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCompletion(id: String, completed: Boolean, completedAt: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET dueDate = :dueDate, dueTimeMinutes = :dueTimeMinutes, updatedAt = :timestamp WHERE id = :id")
    suspend fun rescheduleTask(id: String, dueDate: Long?, dueTimeMinutes: Int?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET projectId = :newProjectId, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveTaskToProject(id: String, newProjectId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET projectId = :newProjectId, updatedAt = :timestamp WHERE projectId = :oldProjectId")
    suspend fun moveAllTasksToProject(oldProjectId: String, newProjectId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTask(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :timestamp WHERE parentTaskId = :parentTaskId")
    suspend fun softDeleteSubtasks(parentTaskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :timestamp WHERE projectId = :projectId")
    suspend fun softDeleteTasksInProject(projectId: String, timestamp: Long = System.currentTimeMillis())
}
