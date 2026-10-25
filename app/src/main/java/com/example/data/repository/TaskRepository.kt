package com.example.data.repository

import com.example.data.local.TasklyDatabase
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.DueDateFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.TaskWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

class TaskRepository(private val db: TasklyDatabase) {
    private val taskDao = db.taskDao()
    private val labelDao = db.labelDao()
    private val projectDao = db.projectDao()
    private val syncQueueDao = db.syncQueueDao()

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksWithDetails(): Flow<List<TaskWithDetails>> {
        return combine(
            taskDao.getAllTasks(),
            labelDao.getAllLabels(),
            projectDao.getAllActiveProjects()
        ) { tasks, labels, projects ->
            val labelMap = labels.associateBy { it.id }
            val projectMap = projects.associateBy { it.id }
            val subtasksByParent = tasks.filter { it.parentTaskId != null }.groupBy { it.parentTaskId!! }

            tasks.filter { it.parentTaskId == null }.map { task ->
                val project = projectMap[task.projectId]
                TaskWithDetails(
                    task = task,
                    subtasks = subtasksByParent[task.id] ?: emptyList(),
                    labels = emptyList(), // Can be enriched per task
                    projectName = project?.name,
                    projectColor = project?.colorHex
                )
            }
        }
    }

    fun getTasksForProject(projectId: String): Flow<List<TaskEntity>> = taskDao.getTasksForProject(projectId)

    fun getOverdueTasks(todayStart: Long): Flow<List<TaskEntity>> = taskDao.getOverdueTasks(todayStart)

    fun getUpcomingTasks(todayStart: Long, endOfWeek: Long): Flow<List<TaskEntity>> = taskDao.getUpcomingTasks(todayStart, endOfWeek)

    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()

    fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>> = taskDao.getSubtasks(parentTaskId)

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    suspend fun insertTask(task: TaskEntity, labelIds: List<String> = emptyList()) {
        taskDao.insertTask(task)
        if (labelIds.isNotEmpty()) {
            labelDao.clearLabelsForTask(task.id)
            for (lblId in labelIds) {
                labelDao.insertTaskLabel(com.example.data.local.entity.TaskLabelCrossRef(task.id, lblId))
            }
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = task.id, operation = "UPSERT", timestamp = task.updatedAt)
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = updated.id, operation = "UPSERT", timestamp = updated.updatedAt)
        )
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val newCompleted = !task.isCompleted
        val now = System.currentTimeMillis()
        val completedAt = if (newCompleted) now else null
        taskDao.updateCompletion(task.id, newCompleted, completedAt, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = task.id, operation = "UPSERT", timestamp = now)
        )

        // Handle Recurring Tasks: If newly completed and has recurrence rule, generate next occurrence
        if (newCompleted && !task.recurrenceRule.isNullOrBlank() && task.dueDate != null) {
            val nextDueDate = calculateNextDueDate(task.dueDate, task.recurrenceRule)
            if (nextDueDate != null) {
                val nextTask = task.copy(
                    id = UUID.randomUUID().toString(),
                    dueDate = nextDueDate,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
                taskDao.insertTask(nextTask)
                syncQueueDao.enqueue(
                    SyncQueueEntity(entityType = "TASK", entityId = nextTask.id, operation = "UPSERT", timestamp = now)
                )
            }
        }
    }

    suspend fun rescheduleTask(taskId: String, dueDate: Long?, dueTimeMinutes: Int?) {
        val now = System.currentTimeMillis()
        taskDao.rescheduleTask(taskId, dueDate, dueTimeMinutes, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = taskId, operation = "UPSERT", timestamp = now)
        )
    }

    suspend fun moveTask(taskId: String, newProjectId: String) {
        val now = System.currentTimeMillis()
        taskDao.moveTaskToProject(taskId, newProjectId, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = taskId, operation = "UPSERT", timestamp = now)
        )
    }

    suspend fun deleteTask(taskId: String) {
        val now = System.currentTimeMillis()
        taskDao.softDeleteTask(taskId, now)
        taskDao.softDeleteSubtasks(taskId, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "TASK", entityId = taskId, operation = "DELETE", timestamp = now)
        )
    }

    private fun calculateNextDueDate(currentDueDate: Long, recurrenceRule: String): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when {
            recurrenceRule.equals("DAILY", ignoreCase = true) -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                return cal.timeInMillis
            }
            recurrenceRule.equals("WEEKLY", ignoreCase = true) -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                return cal.timeInMillis
            }
            recurrenceRule.equals("MONTHLY", ignoreCase = true) -> {
                cal.add(Calendar.MONTH, 1)
                return cal.timeInMillis
            }
            else -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                return cal.timeInMillis
            }
        }
    }
}
