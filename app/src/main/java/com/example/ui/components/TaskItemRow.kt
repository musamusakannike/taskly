package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TaskEntity
import com.example.data.model.Priority
import com.example.data.model.TaskWithDetails
import com.example.ui.theme.Charcoal600
import com.example.ui.theme.Charcoal800
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TasklyRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskItemRow(
    taskDetails: TaskWithDetails,
    onToggleCompletion: (TaskEntity) -> Unit,
    onClick: (TaskEntity) -> Unit,
    onLongClick: ((TaskEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val task = taskDetails.task
    val priorityColor = when (task.priority) {
        1 -> PriorityP1
        2 -> PriorityP2
        3 -> PriorityP3
        else -> PriorityP4
    }

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val isOverdue = task.dueDate != null && task.dueDate < todayStart && !task.isCompleted
    val isDueToday = task.dueDate != null && task.dueDate in todayStart until (todayStart + 86400000L)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick(task) },
                onLongClick = onLongClick?.let { { it(task) } }
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Circular Completion Checkbox
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .testTag("checkbox_${task.id}")
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) SuccessGreen else priorityColor,
                        shape = CircleShape
                    )
                    .background(if (task.isCompleted) SuccessGreen else Color.Transparent)
                    .combinedClickable(onClick = { onToggleCompletion(task) }),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Chips: Date, Time, Project, Subtasks, Recurrence
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Due Date
                    if (task.dueDate != null) {
                        val dateText = when {
                            isOverdue -> "Overdue • " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(task.dueDate))
                            isDueToday -> "Today"
                            task.dueDate in (todayStart + 86400000L) until (todayStart + 2 * 86400000L) -> "Tomorrow"
                            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(task.dueDate))
                        }
                        val dateColor = when {
                            isOverdue -> PriorityP1
                            isDueToday -> PriorityP2
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(dateColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Due date",
                                tint = dateColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = dateColor
                            )
                        }
                    }

                    // Due Time
                    if (task.dueTimeMinutes != null) {
                        val hours = task.dueTimeMinutes / 60
                        val mins = task.dueTimeMinutes % 60
                        val isPm = hours >= 12
                        val displayHour = when {
                            hours == 0 -> 12
                            hours > 12 -> hours - 12
                            else -> hours
                        }
                        val timeStr = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, mins, if (isPm) "PM" else "AM")

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Due time",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Recurrence badge
                    if (!task.recurrenceRule.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = task.recurrenceRule,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subtask progress indicator
                    if (taskDetails.subtaskCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { taskDetails.progress },
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 2.dp,
                                color = TasklyRed
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${taskDetails.completedSubtaskCount}/${taskDetails.subtaskCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Project name badge
                    if (taskDetails.projectName != null && taskDetails.projectName != "Inbox") {
                        val projColor = try {
                            Color(android.graphics.Color.parseColor(taskDetails.projectColor ?: "#DC4C3E"))
                        } catch (_: Exception) {
                            TasklyRed
                        }
                        Text(
                            text = "#${taskDetails.projectName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = projColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(projColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
