package com.example.ai

import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.remote.GeminiClient
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ProposedAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String, // "RESCHEDULE_TASKS", "MOVE_PROJECT", "CREATE_PROJECT", "COMPLETE_TASKS", "CREATE_TASK"
    val title: String,
    val description: String,
    val payloadJson: String
)

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val proposedAction: ProposedAction? = null,
    val isActionApplied: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object AiChatAssistant {
    suspend fun sendMessage(
        userMessage: String,
        allTasks: List<TaskEntity>,
        allProjects: List<ProjectEntity>,
        conversationHistory: List<AiChatMessage>
    ): Result<AiChatMessage> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd (EEEE)", Locale.US).format(Date())

        val tasksContext = JSONArray()
        for (t in allTasks.take(60)) {
            tasksContext.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("priority", "P${t.priority}")
                put("isCompleted", t.isCompleted)
                put("dueDate", t.dueDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) } ?: "none")
                put("projectId", t.projectId)
            })
        }

        val projectsContext = JSONArray()
        for (p in allProjects) {
            projectsContext.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("parentId", p.parentId ?: "none")
            })
        }

        val systemPrompt = """
            You are Taskly's conversational AI task management assistant.
            Today is $todayStr.
            User's Current Projects:
            ${projectsContext.toString()}

            User's Current Tasks:
            ${tasksContext.toString()}

            You can answer questions ("What is due this week?", "What did I complete?") and take action commands ("Move work tasks due today to tomorrow", "Create a project for Japan Trip with 5 tasks").
            Whenever an action is commanded by the user, you MUST return a structured proposed action in JSON so the user can review and approve it via a confirmation card before it runs.
            Return a JSON object:
            {
              "reply": "Your clear, friendly answer to the user",
              "action": {
                "actionType": "RESCHEDULE_TASKS" | "CREATE_PROJECT" | "CREATE_TASK" | "COMPLETE_TASKS",
                "title": "Short title of action",
                "description": "Summary of what will change upon confirmation",
                "parameters": {
                  "taskIds": ["id1", "id2"],
                  "newDueDate": "YYYY-MM-DD" | "tomorrow" | "today",
                  "projectName": "Name",
                  "taskTitles": ["Task 1", "Task 2", "Task 3"]
                }
              }
            }
            If no action is needed (just a question), set "action": null.
        """.trimIndent()

        val conversationPrompt = StringBuilder()
        for (msg in conversationHistory.takeLast(6)) {
            val role = if (msg.isFromUser) "User" else "Assistant"
            conversationPrompt.append("$role: ${msg.text}\n")
        }
        conversationPrompt.append("User: $userMessage\n")

        val result = GeminiClient.generateContent(
            conversationPrompt.toString(),
            systemInstruction = systemPrompt,
            responseMimeType = "application/json"
        )

        return result.mapCatching { jsonStr ->
            val json = JSONObject(jsonStr)
            val replyText = json.optString("reply", "Here is what I found.")
            var proposedAction: ProposedAction? = null

            if (json.has("action") && !json.isNull("action")) {
                val actionObj = json.getJSONObject("action")
                val actionType = actionObj.optString("actionType", "")
                val title = actionObj.optString("title", "Proposed Action")
                val desc = actionObj.optString("description", "")
                val params = actionObj.optJSONObject("parameters")?.toString() ?: "{}"
                if (actionType.isNotBlank()) {
                    proposedAction = ProposedAction(
                        actionType = actionType,
                        title = title,
                        description = desc,
                        payloadJson = params
                    )
                }
            }

            AiChatMessage(
                isFromUser = false,
                text = replyText,
                proposedAction = proposedAction
            )
        }
    }
}
