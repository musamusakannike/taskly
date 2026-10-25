package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TasklyApplication
import com.example.ai.NaturalLanguageParser
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.DueDateFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.ProjectTreeItem
import com.example.data.model.SyncStatus
import com.example.data.model.TaskWithDetails
import com.example.notifications.AlarmScheduler
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TasklyApplication
    private val taskRepo = app.taskRepository
    private val projectRepo = app.projectRepository
    private val labelRepo = app.labelRepository
    private val syncEngine = app.syncEngine
    private val authManager = app.authManager

    val syncStatus: StateFlow<SyncStatus> = syncEngine.syncStatus
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    val projects: StateFlow<List<ProjectEntity>> = projectRepo.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectTree: StateFlow<List<ProjectTreeItem>> = projectRepo.getProjectTree()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labels: StateFlow<List<LabelEntity>> = labelRepo.getAllLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasksWithDetails: StateFlow<List<TaskWithDetails>> = taskRepo.getTasksWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today & Overdue bounds
    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val todayTasks: StateFlow<List<TaskWithDetails>> = allTasksWithDetails.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val todayStart = getTodayStartMillis()
        val todayEnd = todayStart + 86400000L
        tasks.filter { details ->
            val due = details.task.dueDate
            due != null && due >= todayStart && due < todayEnd && !details.task.isCompleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTasks: StateFlow<List<TaskWithDetails>> = allTasksWithDetails.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val todayStart = getTodayStartMillis()
        tasks.filter { details ->
            val due = details.task.dueDate
            due != null && due < todayStart && !details.task.isCompleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<TaskWithDetails>> = allTasksWithDetails.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val todayStart = getTodayStartMillis()
        val endOfWeek = todayStart + 7 * 86400000L
        tasks.filter { details ->
            val due = details.task.dueDate
            due != null && due >= todayStart && due <= endOfWeek && !details.task.isCompleted
        }.sortedBy { it.task.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<TaskWithDetails>> = allTasksWithDetails.combine(MutableStateFlow(Unit)) { tasks, _ ->
        tasks.filter { it.task.isCompleted }.sortedByDescending { it.task.completedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Filter
    val filterCriteria = MutableStateFlow(FilterCriteria())

    // Search query
    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<TaskEntity>> = searchQuery.flatMapLatest { q ->
        taskRepo.searchTasks(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Snackbar / Undo
    private val _lastDeletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun syncNow() {
        syncEngine.syncNow()
    }

    fun signInWithGoogle(activity: Activity, onError: (String) -> Unit) {
        authManager.signInWithGoogle(
            activity,
            onSuccess = {
                syncEngine.startListening()
                syncEngine.syncNow()
            },
            onError = onError
        )
    }

    fun signOut() {
        authManager.signOut {
            syncEngine.stopListening()
        }
    }

    // Task Actions
    fun createTask(
        title: String,
        description: String = "",
        priority: Int = 4,
        dueDate: Long? = null,
        dueTimeMinutes: Int? = null,
        recurrenceRule: String? = null,
        projectId: String? = null,
        labelIds: List<String> = emptyList(),
        parentTaskId: String? = null,
        estimatedMinutes: Int? = null,
        reminderMillis: Long? = null
    ) {
        viewModelScope.launch {
            val inboxProject = projects.value.firstOrNull { it.isInbox }
            val targetProjectId = projectId ?: inboxProject?.id ?: projects.value.firstOrNull()?.id ?: "inbox"
            val taskId = UUID.randomUUID().toString()

            val task = TaskEntity(
                id = taskId,
                projectId = targetProjectId,
                parentTaskId = parentTaskId,
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                dueTimeMinutes = dueTimeMinutes,
                isAllDay = dueTimeMinutes == null,
                recurrenceRule = recurrenceRule,
                estimatedMinutes = estimatedMinutes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            taskRepo.insertTask(task, labelIds)

            if (reminderMillis != null && reminderMillis > System.currentTimeMillis()) {
                AlarmScheduler.scheduleReminder(app, taskId, reminderMillis)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.toggleTaskCompletion(task)
            _snackMessage.value = if (!task.isCompleted) "Task completed" else "Task uncompleted"
        }
    }

    fun rescheduleTask(taskId: String, dueDate: Long?, dueTimeMinutes: Int?) {
        viewModelScope.launch {
            taskRepo.rescheduleTask(taskId, dueDate, dueTimeMinutes)
            _snackMessage.value = "Task rescheduled"
        }
    }

    fun moveTask(taskId: String, newProjectId: String) {
        viewModelScope.launch {
            taskRepo.moveTask(taskId, newProjectId)
            _snackMessage.value = "Task moved"
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastDeletedTask.value = task
            taskRepo.deleteTask(task.id)
            _snackMessage.value = "Task deleted"
        }
    }

    fun undoLastDelete() {
        val task = _lastDeletedTask.value ?: return
        viewModelScope.launch {
            taskRepo.insertTask(task.copy(isDeleted = false, updatedAt = System.currentTimeMillis()))
            _lastDeletedTask.value = null
            _snackMessage.value = "Action undone"
        }
    }

    // Project Actions
    fun createProject(name: String, parentId: String? = null, colorHex: String = "#DC4C3E", isFavorite: Boolean = false) {
        viewModelScope.launch {
            projectRepo.createProject(name, parentId, colorHex, isFavorite)
        }
    }

    fun toggleFavorite(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepo.toggleFavorite(project.id, !project.isFavorite)
        }
    }

    fun toggleArchive(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepo.toggleArchive(project.id, !project.isArchived)
        }
    }

    fun deleteProject(project: ProjectEntity, moveContentsToParent: Boolean) {
        viewModelScope.launch {
            projectRepo.deleteProject(project, moveContentsToParent)
            _snackMessage.value = "Project deleted"
        }
    }

    // Label Actions
    fun createLabel(name: String, colorHex: String) {
        viewModelScope.launch {
            labelRepo.createLabel(name, colorHex)
        }
    }

    fun deleteLabel(labelId: String) {
        viewModelScope.launch {
            labelRepo.deleteLabel(labelId)
        }
    }
}
