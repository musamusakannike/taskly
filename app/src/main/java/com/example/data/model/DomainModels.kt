package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4

enum class Priority(val level: Int, val label: String, val color: Color) {
    P1(1, "P1", PriorityP1),
    P2(2, "P2", PriorityP2),
    P3(3, "P3", PriorityP3),
    P4(4, "P4", PriorityP4);

    companion object {
        fun fromLevel(level: Int): Priority = when (level) {
            1 -> P1
            2 -> P2
            3 -> P3
            else -> P4
        }
    }
}

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class TaskWithDetails(
    val task: TaskEntity,
    val subtasks: List<TaskEntity> = emptyList(),
    val labels: List<LabelEntity> = emptyList(),
    val projectName: String? = null,
    val projectColor: String? = null
) {
    val subtaskCount: Int get() = subtasks.size
    val completedSubtaskCount: Int get() = subtasks.count { it.isCompleted }
    val progress: Float get() = if (subtaskCount > 0) completedSubtaskCount.toFloat() / subtaskCount.toFloat() else 0f
}

data class ProjectTreeItem(
    val project: ProjectEntity,
    val depth: Int = 0,
    val taskCount: Int = 0,
    val hasChildren: Boolean = false,
    val isExpanded: Boolean = true
)

enum class DueDateFilter {
    ALL,
    OVERDUE,
    TODAY,
    THIS_WEEK,
    NO_DATE
}

data class FilterCriteria(
    val priority: Priority? = null,
    val labelId: String? = null,
    val projectId: String? = null,
    val dueDateFilter: DueDateFilter = DueDateFilter.ALL,
    val showCompleted: Boolean = false
)
