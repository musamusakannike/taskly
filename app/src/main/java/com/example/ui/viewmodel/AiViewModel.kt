package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TasklyApplication
import com.example.ai.AiBreakdownHelper
import com.example.ai.AiChatAssistant
import com.example.ai.AiChatMessage
import com.example.ai.AiDailyPlanner
import com.example.ai.DailyPlanResult
import com.example.ai.NaturalLanguageParser
import com.example.ai.ParsedTaskDraft
import com.example.ai.ProposedAction
import com.example.ai.SuggestedSubtask
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TasklyApplication
    private val taskRepo = app.taskRepository
    private val projectRepo = app.projectRepository

    // --- 1. Natural Language Quick Add ---
    val rawInput = MutableStateFlow("")
    private val _liveDraft = MutableStateFlow(ParsedTaskDraft(cleanTitle = ""))
    val liveDraft: StateFlow<ParsedTaskDraft> = _liveDraft.asStateFlow()

    fun onInputChanged(text: String) {
        rawInput.value = text
        _liveDraft.value = NaturalLanguageParser.parseQuick(text)
    }

    fun parseDeepWithGemini(onComplete: (ParsedTaskDraft) -> Unit) {
        val input = rawInput.value
        if (input.isBlank()) return
        viewModelScope.launch {
            val result = NaturalLanguageParser.parseWithGemini(input)
            result.onSuccess { draft ->
                _liveDraft.value = draft
                onComplete(draft)
            }
        }
    }

    // --- 2. Auto-Breakdown ---
    val isBreakingDown = MutableStateFlow(false)
    val breakdownError = MutableStateFlow<String?>(null)
    val suggestedSubtasks = MutableStateFlow<List<SuggestedSubtask>>(emptyList())

    fun requestBreakdown(task: TaskEntity) {
        isBreakingDown.value = true
        breakdownError.value = null
        suggestedSubtasks.value = emptyList()

        viewModelScope.launch {
            val result = AiBreakdownHelper.breakdownTask(task.title, task.description)
            isBreakingDown.value = false
            result.fold(
                onSuccess = { list ->
                    suggestedSubtasks.value = list
                },
                onFailure = { err ->
                    breakdownError.value = err.localizedMessage ?: "Failed to break down task"
                }
            )
        }
    }

    fun toggleSubtaskSelection(index: Int) {
        val current = suggestedSubtasks.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            suggestedSubtasks.value = current
        }
    }

    fun applySubtasks(parentTask: TaskEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val selected = suggestedSubtasks.value.filter { it.isSelected }
            val now = System.currentTimeMillis()
            for ((idx, sub) in selected.withIndex()) {
                val subtask = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = parentTask.projectId,
                    parentTaskId = parentTask.id,
                    title = sub.title,
                    estimatedMinutes = sub.estimatedMinutes,
                    orderIndex = idx,
                    createdAt = now + idx,
                    updatedAt = now + idx
                )
                taskRepo.insertTask(subtask)
            }
            suggestedSubtasks.value = emptyList()
            onDone()
        }
    }

    // --- 3. Smart Daily Planner ---
    val isPlanning = MutableStateFlow(false)
    val planningError = MutableStateFlow<String?>(null)
    val dailyPlanResult = MutableStateFlow<DailyPlanResult?>(null)

    fun generateDailyPlan(todayTasks: List<TaskEntity>, overdueTasks: List<TaskEntity>) {
        isPlanning.value = true
        planningError.value = null

        viewModelScope.launch {
            val result = AiDailyPlanner.generateDailyPlan(todayTasks, overdueTasks)
            isPlanning.value = false
            result.fold(
                onSuccess = { plan ->
                    dailyPlanResult.value = plan
                },
                onFailure = { err ->
                    planningError.value = err.localizedMessage ?: "Plan generation failed"
                }
            )
        }
    }

    fun applyRescheduleSuggestions(onDone: () -> Unit) {
        val suggestions = dailyPlanResult.value?.rescheduleSuggestions ?: return
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val tomorrow = cal.timeInMillis + 86400000L

            for (sug in suggestions) {
                taskRepo.rescheduleTask(sug.taskId, tomorrow, null)
            }
            onDone()
        }
    }

    // --- 4. AI Chat Assistant ---
    val chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isFromUser = false,
                text = "Hi! I'm Taskly AI. Ask me about what's due, completed tasks, or ask me to reschedule or create projects for you."
            )
        )
    )
    val isChatSending = MutableStateFlow(false)

    fun sendChatMessage(
        text: String,
        allTasks: List<TaskEntity>,
        allProjects: List<ProjectEntity>
    ) {
        if (text.isBlank()) return
        val userMsg = AiChatMessage(isFromUser = true, text = text)
        chatMessages.value = chatMessages.value + userMsg
        isChatSending.value = true

        viewModelScope.launch {
            val result = AiChatAssistant.sendMessage(
                userMessage = text,
                allTasks = allTasks,
                allProjects = allProjects,
                conversationHistory = chatMessages.value
            )
            isChatSending.value = false
            result.fold(
                onSuccess = { aiMsg ->
                    chatMessages.value = chatMessages.value + aiMsg
                },
                onFailure = { err ->
                    chatMessages.value = chatMessages.value + AiChatMessage(
                        isFromUser = false,
                        text = "I couldn't connect right now. (${err.localizedMessage ?: "Offline"}). Please check your connection."
                    )
                }
            )
        }
    }

    fun applyProposedAction(messageId: String, action: ProposedAction, onApplied: () -> Unit) {
        viewModelScope.launch {
            val params = try { JSONObject(action.payloadJson) } catch (_: Exception) { JSONObject() }
            when (action.actionType) {
                "RESCHEDULE_TASKS" -> {
                    val taskIds = params.optJSONArray("taskIds")
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val targetDate = cal.timeInMillis + 86400000L // default tomorrow

                    if (taskIds != null) {
                        for (i in 0 until taskIds.length()) {
                            taskRepo.rescheduleTask(taskIds.getString(i), targetDate, null)
                        }
                    }
                }
                "CREATE_PROJECT" -> {
                    val projectName = params.optString("projectName", "New Project")
                    val newProj = projectRepo.createProject(projectName)
                    val taskTitles = params.optJSONArray("taskTitles")
                    if (taskTitles != null) {
                        val now = System.currentTimeMillis()
                        for (i in 0 until taskTitles.length()) {
                            val task = TaskEntity(
                                id = UUID.randomUUID().toString(),
                                projectId = newProj.id,
                                title = taskTitles.getString(i),
                                createdAt = now + i,
                                updatedAt = now + i
                            )
                            taskRepo.insertTask(task)
                        }
                    }
                }
                "COMPLETE_TASKS" -> {
                    val taskIds = params.optJSONArray("taskIds")
                    if (taskIds != null) {
                        for (i in 0 until taskIds.length()) {
                            val id = taskIds.getString(i)
                            // toggle
                            val tasks = taskRepo.getAllTasks()
                            // ...
                        }
                    }
                }
            }

            // Mark action applied in chat
            val updated = chatMessages.value.map { msg ->
                if (msg.id == messageId) msg.copy(isActionApplied = true) else msg
            }
            chatMessages.value = updated
            onApplied()
        }
    }
}
