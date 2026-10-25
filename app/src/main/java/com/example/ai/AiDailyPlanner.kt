package com.example.ai

import com.example.data.local.entity.TaskEntity
import com.example.data.remote.GeminiClient
import org.json.JSONArray
import org.json.JSONObject

data class ScheduledTimeBlock(
    val taskId: String,
    val taskTitle: String,
    val timeSlot: String, // e.g. "09:00 - 09:45"
    val durationMinutes: Int,
    val reason: String
)

data class RescheduleSuggestion(
    val taskId: String,
    val taskTitle: String,
    val suggestedDate: String, // e.g. "Tomorrow"
    val reason: String
)

data class DailyPlanResult(
    val timeBlocks: List<ScheduledTimeBlock>,
    val rescheduleSuggestions: List<RescheduleSuggestion>,
    val motivationalQuote: String
)

object AiDailyPlanner {
    suspend fun generateDailyPlan(todayTasks: List<TaskEntity>, overdueTasks: List<TaskEntity>): Result<DailyPlanResult> {
        val tasksSummary = JSONArray()
        for (t in overdueTasks) {
            tasksSummary.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("priority", "P${t.priority}")
                put("status", "OVERDUE")
                put("estimatedMinutes", t.estimatedMinutes ?: 30)
            })
        }
        for (t in todayTasks) {
            tasksSummary.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("priority", "P${t.priority}")
                put("status", "DUE_TODAY")
                put("dueTimeMinutes", t.dueTimeMinutes ?: -1)
                put("estimatedMinutes", t.estimatedMinutes ?: 30)
            })
        }

        val systemPrompt = """
            You are the "Plan My Day" engine for Taskly, an intelligent schedule optimizer.
            Given the user's tasks for today and overdue tasks, prioritize them and allocate them into a realistic workday schedule starting from 9:00 AM.
            If there are too many low priority tasks or overdue backlog items that will cause burnout, suggest rescheduling them to "Tomorrow" or "Next week".
            Return a JSON object:
            {
              "schedule": [
                {
                  "taskId": "task-id",
                  "taskTitle": "Title",
                  "timeSlot": "09:00 - 09:45",
                  "durationMinutes": 45,
                  "reason": "High priority focus session"
                }
              ],
              "rescheduleSuggestions": [
                {
                  "taskId": "task-id",
                  "taskTitle": "Title",
                  "suggestedDate": "Tomorrow",
                  "reason": "Lower priority chore best deferred to protect focus time"
                }
              ],
              "motivationalQuote": "A short energizing 1-sentence productivity tip."
            }
        """.trimIndent()

        val prompt = "Here are my tasks for today:\n${tasksSummary.toString(2)}"
        val result = GeminiClient.generateContent(prompt, systemInstruction = systemPrompt, responseMimeType = "application/json")

        return result.mapCatching { jsonStr ->
            val json = JSONObject(jsonStr)
            val scheduleArray = json.optJSONArray("schedule") ?: JSONArray()
            val timeBlocks = mutableListOf<ScheduledTimeBlock>()
            for (i in 0 until scheduleArray.length()) {
                val item = scheduleArray.getJSONObject(i)
                timeBlocks.add(
                    ScheduledTimeBlock(
                        taskId = item.optString("taskId"),
                        taskTitle = item.optString("taskTitle"),
                        timeSlot = item.optString("timeSlot", "09:00 - 09:30"),
                        durationMinutes = item.optInt("durationMinutes", 30),
                        reason = item.optString("reason", "Focus block")
                    )
                )
            }

            val reschedArray = json.optJSONArray("rescheduleSuggestions") ?: JSONArray()
            val reschedSuggestions = mutableListOf<RescheduleSuggestion>()
            for (i in 0 until reschedArray.length()) {
                val item = reschedArray.getJSONObject(i)
                reschedSuggestions.add(
                    RescheduleSuggestion(
                        taskId = item.optString("taskId"),
                        taskTitle = item.optString("taskTitle"),
                        suggestedDate = item.optString("suggestedDate", "Tomorrow"),
                        reason = item.optString("reason", "Deferred to balance schedule")
                    )
                )
            }

            DailyPlanResult(
                timeBlocks = timeBlocks,
                rescheduleSuggestions = reschedSuggestions,
                motivationalQuote = json.optString("motivationalQuote", "Focus on one high-impact task at a time.")
            )
        }
    }
}
