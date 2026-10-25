package com.example.ai

import com.example.data.remote.GeminiClient
import org.json.JSONArray
import org.json.JSONObject

data class SuggestedSubtask(
    val title: String,
    val estimatedMinutes: Int = 15,
    val isSelected: Boolean = true
)

object AiBreakdownHelper {
    suspend fun breakdownTask(taskTitle: String, taskDescription: String?): Result<List<SuggestedSubtask>> {
        val systemPrompt = """
            You are a productivity expert assistant for Taskly.
            Given a parent task title and description, break it down into 3 to 6 logical, concrete, and actionable subtasks.
            Return a JSON array of objects:
            [
              {
                "title": "Subtask title",
                "estimatedMinutes": 20
              }
            ]
        """.trimIndent()

        val prompt = "Break down this task: Title: \"$taskTitle\". Details: \"${taskDescription ?: ""}\""
        val result = GeminiClient.generateContent(prompt, systemInstruction = systemPrompt, responseMimeType = "application/json")

        return result.mapCatching { jsonStr ->
            val list = mutableListOf<SuggestedSubtask>()
            val trimmed = jsonStr.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("subtasks") ?: obj.optJSONArray("items") ?: JSONArray()
            } else {
                JSONArray()
            }

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val title = item.optString("title", "").trim()
                val minutes = item.optInt("estimatedMinutes", 15)
                if (title.isNotBlank()) {
                    list.add(SuggestedSubtask(title = title, estimatedMinutes = minutes))
                }
            }

            if (list.isEmpty()) {
                listOf(
                    SuggestedSubtask("Plan and outline requirements", 15),
                    SuggestedSubtask("Execute primary implementation", 45),
                    SuggestedSubtask("Review results and verify completion", 15)
                )
            } else {
                list
            }
        }
    }
}
