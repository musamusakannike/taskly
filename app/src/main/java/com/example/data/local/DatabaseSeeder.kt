package com.example.data.local

import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskLabelCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

object DatabaseSeeder {
    suspend fun seedIfNeeded(db: TasklyDatabase) = withContext(Dispatchers.IO) {
        val existingInbox = db.projectDao().getInboxProjectSync()
        if (existingInbox != null) return@withContext

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        // 1. Projects
        val inboxId = UUID.randomUUID().toString()
        val workId = UUID.randomUUID().toString()
        val clientAId = UUID.randomUUID().toString()
        val redesignId = UUID.randomUUID().toString()
        val personalId = UUID.randomUUID().toString()
        val healthId = UUID.randomUUID().toString()

        val projects = listOf(
            ProjectEntity(
                id = inboxId,
                name = "Inbox",
                colorHex = "#246FE0",
                iconName = "inbox",
                isInbox = true,
                isFavorite = true,
                sortOrder = 0
            ),
            ProjectEntity(
                id = workId,
                name = "Work",
                colorHex = "#DC4C3E",
                iconName = "work",
                isFavorite = true,
                sortOrder = 1
            ),
            ProjectEntity(
                id = clientAId,
                parentId = workId,
                name = "Client Acme",
                colorHex = "#EB8909",
                sortOrder = 0
            ),
            ProjectEntity(
                id = redesignId,
                parentId = clientAId,
                name = "Website Redesign",
                colorHex = "#96C3EB",
                sortOrder = 0
            ),
            ProjectEntity(
                id = personalId,
                name = "Personal",
                colorHex = "#058527",
                iconName = "person",
                isFavorite = false,
                sortOrder = 2
            ),
            ProjectEntity(
                id = healthId,
                parentId = personalId,
                name = "Fitness & Health",
                colorHex = "#14AAF5",
                sortOrder = 0
            )
        )
        db.projectDao().insertProjects(projects)

        // 2. Labels
        val urgentLabelId = UUID.randomUUID().toString()
        val clientLabelId = UUID.randomUUID().toString()
        val phoneLabelId = UUID.randomUUID().toString()
        val readingLabelId = UUID.randomUUID().toString()

        val labels = listOf(
            LabelEntity(id = urgentLabelId, name = "urgent", colorHex = "#DE4C4A"),
            LabelEntity(id = clientLabelId, name = "client", colorHex = "#F49C18"),
            LabelEntity(id = phoneLabelId, name = "phone", colorHex = "#4073D6"),
            LabelEntity(id = readingLabelId, name = "reading", colorHex = "#8F9499")
        )
        db.labelDao().insertLabels(labels)

        // 3. Tasks
        val task1Id = UUID.randomUUID().toString()
        val subtask1aId = UUID.randomUUID().toString()
        val subtask1bId = UUID.randomUUID().toString()
        val subtask1cId = UUID.randomUUID().toString()

        val task2Id = UUID.randomUUID().toString()
        val task3Id = UUID.randomUUID().toString()
        val task4Id = UUID.randomUUID().toString()
        val task5Id = UUID.randomUUID().toString()

        val tasks = listOf(
            // Today P1 task with subtasks
            TaskEntity(
                id = task1Id,
                projectId = redesignId,
                title = "Launch client website landing page",
                description = "Final QA audit and deployment checklist before Friday demo.",
                priority = 1,
                dueDate = todayStart,
                dueTimeMinutes = 15 * 60, // 3:00 PM
                isAllDay = false,
                estimatedMinutes = 90
            ),
            TaskEntity(
                id = subtask1aId,
                projectId = redesignId,
                parentTaskId = task1Id,
                title = "Verify mobile responsive navigation",
                isCompleted = true,
                completedAt = now - 3600000L,
                orderIndex = 0
            ),
            TaskEntity(
                id = subtask1bId,
                projectId = redesignId,
                parentTaskId = task1Id,
                title = "Check lighthouse performance scores (>90)",
                isCompleted = false,
                orderIndex = 1
            ),
            TaskEntity(
                id = subtask1cId,
                projectId = redesignId,
                parentTaskId = task1Id,
                title = "Deploy to production cloud server",
                isCompleted = false,
                orderIndex = 2
            ),

            // Overdue task
            TaskEntity(
                id = task2Id,
                projectId = workId,
                title = "Send monthly invoice to Acme Corp",
                description = "Attach invoice PDF and time breakdown sheet.",
                priority = 2,
                dueDate = todayStart - 86400000L, // Yesterday
                dueTimeMinutes = 11 * 60,
                isAllDay = false
            ),

            // Recurring task
            TaskEntity(
                id = task3Id,
                projectId = healthId,
                title = "Morning 5km run & stretching",
                description = "Keep heart rate in zone 2.",
                priority = 3,
                dueDate = todayStart,
                dueTimeMinutes = 7 * 60,
                recurrenceRule = "DAILY",
                isCompleted = false
            ),

            // Inbox task
            TaskEntity(
                id = task4Id,
                projectId = inboxId,
                title = "Review design system guidelines document",
                priority = 4,
                dueDate = todayStart + 86400000L // Tomorrow
            ),

            // Upcoming task
            TaskEntity(
                id = task5Id,
                projectId = personalId,
                title = "Plan weekend hiking trip itinerary",
                description = "Pick trail, check weather forecast, pack gear.",
                priority = 2,
                dueDate = todayStart + 3 * 86400000L
            )
        )
        db.taskDao().insertTasks(tasks)

        // 4. Attach cross-ref labels
        db.labelDao().insertTaskLabels(
            listOf(
                TaskLabelCrossRef(task1Id, urgentLabelId),
                TaskLabelCrossRef(task1Id, clientLabelId),
                TaskLabelCrossRef(task2Id, clientLabelId),
                TaskLabelCrossRef(task4Id, readingLabelId)
            )
        )
    }
}
