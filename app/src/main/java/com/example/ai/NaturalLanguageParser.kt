package com.example.ai

import com.example.data.remote.GeminiClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedTaskDraft(
    val cleanTitle: String,
    val priority: Int = 4,
    val projectName: String? = null,
    val labelNames: List<String> = emptyList(),
    val dueDateEpochMillis: Long? = null,
    val dueTimeMinutes: Int? = null,
    val recurrenceRule: String? = null
)

object NaturalLanguageParser {

    /**
     * Fast local synchronous tokenizer: 0ms latency for typing in the text field.
     */
    fun parseQuick(rawText: String): ParsedTaskDraft {
        var text = rawText.trim()
        var priority = 4
        var projectName: String? = null
        val labels = mutableListOf<String>()
        var dueDate: Long? = null
        var dueTimeMinutes: Int? = null
        var recurrence: String? = null

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        // 1. Priority regex: p1, p2, p3, p4, !p1, etc.
        val priorityRegex = Regex("""(?i)\b(?:!|p)([1-4])\b""")
        val priorityMatch = priorityRegex.find(text)
        if (priorityMatch != null) {
            priority = priorityMatch.groupValues[1].toInt()
            text = text.replace(priorityMatch.value, "").trim()
        }

        // 2. Project regex: #ProjectName
        val projectRegex = Regex("""#([A-Za-z0-9_-]+)""")
        val projectMatch = projectRegex.find(text)
        if (projectMatch != null) {
            projectName = projectMatch.groupValues[1]
            text = text.replace(projectMatch.value, "").trim()
        }

        // 3. Label regex: @LabelName
        val labelRegex = Regex("""@([A-Za-z0-9_-]+)""")
        val labelMatches = labelRegex.findAll(text)
        for (match in labelMatches) {
            labels.add(match.groupValues[1])
            text = text.replace(match.value, "").trim()
        }

        // 4. Quick dates
        val lowerText = text.lowercase()
        when {
            lowerText.contains("today") -> {
                dueDate = todayStart
                text = text.replace(Regex("(?i)\\btoday\\b"), "").trim()
            }
            lowerText.contains("tomorrow") -> {
                dueDate = todayStart + 86400000L
                text = text.replace(Regex("(?i)\\btomorrow\\b"), "").trim()
            }
            lowerText.contains("next week") -> {
                dueDate = todayStart + 7 * 86400000L
                text = text.replace(Regex("(?i)\\bnext week\\b"), "").trim()
            }
        }

        // 5. Time regex: e.g. 5pm, 5:30pm, 15:00, 9am
        val timeRegex = Regex("""(?i)\b([0-1]?[0-9]|2[0-3])(?::([0-5][0-9]))?\s*(am|pm)\b""")
        val timeMatch = timeRegex.find(text)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toInt()
            val min = if (timeMatch.groupValues[2].isNotBlank()) timeMatch.groupValues[2].toInt() else 0
            val amPm = timeMatch.groupValues[3].lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            dueTimeMinutes = hour * 60 + min
            if (dueDate == null) dueDate = todayStart
            text = text.replace(timeMatch.value, "").trim()
        }

        // 6. Recurrence keywords
        when {
            lowerText.contains("every day") || lowerText.contains("daily") -> {
                recurrence = "DAILY"
                text = text.replace(Regex("(?i)\\b(every day|daily)\\b"), "").trim()
            }
            lowerText.contains("every week") || lowerText.contains("weekly") -> {
                recurrence = "WEEKLY"
                text = text.replace(Regex("(?i)\\b(every week|weekly)\\b"), "").trim()
            }
            lowerText.contains("every month") || lowerText.contains("monthly") -> {
                recurrence = "MONTHLY"
                text = text.replace(Regex("(?i)\\b(every month|monthly)\\b"), "").trim()
            }
        }

        // Clean extra whitespace
        val cleanTitle = text.replace(Regex("""\s+"""), " ").trim()

        return ParsedTaskDraft(
            cleanTitle = if (cleanTitle.isNotBlank()) cleanTitle else rawText,
            priority = priority,
            projectName = projectName,
            labelNames = labels,
            dueDateEpochMillis = dueDate,
            dueTimeMinutes = dueTimeMinutes,
            recurrenceRule = recurrence
        )
    }

    /**
     * AI Deep Parsing via Gemini for complex expressions.
     */
    suspend fun parseWithGemini(input: String): Result<ParsedTaskDraft> {
        val quick = parseQuick(input)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val systemPrompt = """
            You are an expert natural language task parser.
            Today is $todayStr.
            Given user input, return a strict JSON object with:
            {
              "title": string,
              "priority": integer (1 for urgent P1, 2 for high P2, 3 for medium P3, 4 for normal P4),
              "projectName": string or null,
              "labels": array of strings,
              "dueDate": "YYYY-MM-DD" or null,
              "dueTime": "HH:mm" (24h) or null,
              "recurrenceRule": "DAILY", "WEEKLY", "MONTHLY", or null
            }
        """.trimIndent()

        val prompt = "Parse this task entry: \"$input\""
        val result = GeminiClient.generateContent(prompt, systemInstruction = systemPrompt, responseMimeType = "application/json")

        return result.mapCatching { jsonStr ->
            val json = JSONObject(jsonStr)
            val title = json.optString("title", quick.cleanTitle)
            val prio = json.optInt("priority", quick.priority)
            val proj = if (json.has("projectName") && !json.isNull("projectName")) json.optString("projectName") else quick.projectName
            val labelsList = mutableListOf<String>()
            val labelsArray = json.optJSONArray("labels")
            if (labelsArray != null) {
                for (i in 0 until labelsArray.length()) {
                    labelsList.add(labelsArray.getString(i))
                }
            } else {
                labelsList.addAll(quick.labelNames)
            }

            var dueDateMillis = quick.dueDateEpochMillis
            val dateStr = json.optString("dueDate", "")
            if (dateStr.isNotBlank() && dateStr != "null") {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    dueDateMillis = sdf.parse(dateStr)?.time ?: quick.dueDateEpochMillis
                } catch (_: Exception) {}
            }

            var timeMinutes = quick.dueTimeMinutes
            val timeStr = json.optString("dueTime", "")
            if (timeStr.isNotBlank() && timeStr != "null") {
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    timeMinutes = parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0)
                }
            }

            val recurrence = if (json.has("recurrenceRule") && !json.isNull("recurrenceRule")) json.optString("recurrenceRule") else quick.recurrenceRule

            ParsedTaskDraft(
                cleanTitle = title,
                priority = prio,
                projectName = proj,
                labelNames = labelsList,
                dueDateEpochMillis = dueDateMillis,
                dueTimeMinutes = timeMinutes,
                recurrenceRule = recurrence
            )
        }
    }
}
