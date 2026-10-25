package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.Priority
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: TaskEntity,
    taskViewModel: TaskViewModel,
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(task) { mutableStateOf(task.title) }
    var description by remember(task) { mutableStateOf(task.description) }
    var priority by remember(task) { mutableStateOf(task.priority) }
    var dueDate by remember(task) { mutableStateOf(task.dueDate) }
    var dueTimeMinutes by remember(task) { mutableStateOf(task.dueTimeMinutes) }
    var recurrenceRule by remember(task) { mutableStateOf(task.recurrenceRule) }
    var projectId by remember(task) { mutableStateOf(task.projectId) }

    val allTasks by taskViewModel.allTasksWithDetails.collectAsState()
    val projects by taskViewModel.projects.collectAsState()

    // Subtasks for this task
    val subtasks = remember(allTasks, task.id) {
        allTasks.filter { it.task.parentTaskId == task.id }.map { it.task }
    }

    var newSubtaskTitle by remember { mutableStateOf("") }
    var newCommentText by remember { mutableStateOf("") }
    var commentsList by remember {
        mutableStateOf(
            listOf(
                CommentEntity(
                    id = "c1",
                    taskId = task.id,
                    content = "Task logged.",
                    authorName = "You",
                    createdAt = task.createdAt
                )
            )
        )
    }

    var showAiBreakdownDialog by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }

    val isBreakingDown by aiViewModel.isBreakingDown.collectAsState()
    val suggestedSubtasks by aiViewModel.suggestedSubtasks.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Project tag, completion status, close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Project picker button
                Box {
                    val currentProj = projects.firstOrNull { it.id == projectId }
                    Text(
                        text = "#${currentProj?.name ?: "Inbox"}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TasklyRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TasklyRed.copy(alpha = 0.1f))
                            .clickable { projectMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )

                    DropdownMenu(
                        expanded = projectMenuExpanded,
                        onDismissRequest = { projectMenuExpanded = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    projectId = p.id
                                    taskViewModel.moveTask(task.id, p.id)
                                    projectMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            taskViewModel.deleteTask(task)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    taskViewModel.updateTask(task.copy(title = it))
                },
                placeholder = { Text("Task title") },
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().testTag("edit_task_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TasklyRed,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Markdown Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    taskViewModel.updateTask(task.copy(description = it))
                },
                placeholder = { Text("Description / Markdown notes...") },
                modifier = Modifier.fillMaxWidth().testTag("edit_task_description"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TasklyRed,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick chips: Priority, Date, Recurrence
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority
                val pColor = when (priority) {
                    1 -> PriorityP1
                    2 -> PriorityP2
                    3 -> PriorityP3
                    else -> PriorityP4
                }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pColor.copy(alpha = 0.15f))
                            .clickable { priorityMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = pColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("P$priority", color = pColor, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false }
                    ) {
                        listOf(1 to PriorityP1, 2 to PriorityP2, 3 to PriorityP3, 4 to PriorityP4).forEach { (p, col) ->
                            DropdownMenuItem(
                                text = { Text("P$p Priority", color = col, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    priority = p
                                    taskViewModel.updateTask(task.copy(priority = p))
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Due Date Quick Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val newDue = (dueDate ?: cal.timeInMillis) + 86400000L
                            dueDate = newDue
                            taskViewModel.rescheduleTask(task.id, newDue, dueTimeMinutes)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (dueDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueDate!!)) else "Set Date",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Recurrence
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { recurrenceMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(recurrenceRule ?: "No Repeat", style = MaterialTheme.typography.labelMedium)
                    }

                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false }
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", null).forEach { rule ->
                            DropdownMenuItem(
                                text = { Text(rule ?: "No Repeat") },
                                onClick = {
                                    recurrenceRule = rule
                                    taskViewModel.updateTask(task.copy(recurrenceRule = rule))
                                    recurrenceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Subtasks Section Header + AI Auto-Breakdown button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtasks (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // "Break down with AI" button
                Button(
                    onClick = {
                        aiViewModel.requestBreakdown(task)
                        showAiBreakdownDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TasklyRed.copy(alpha = 0.12f),
                        contentColor = TasklyRed
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("break_down_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Break down with AI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Subtask list
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = sub.isCompleted,
                            onCheckedChange = { taskViewModel.toggleTaskCompletion(sub) },
                            colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                        )
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { taskViewModel.deleteTask(sub) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete subtask", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Add subtask input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Add subtask...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TasklyRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                taskViewModel.createTask(
                                    title = newSubtaskTitle.trim(),
                                    projectId = task.projectId,
                                    parentTaskId = task.id
                                )
                                newSubtaskTitle = ""
                            }
                        },
                        enabled = newSubtaskTitle.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Comments
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            commentsList.forEach { comment ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(comment.authorName.take(1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = comment.content,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // New comment field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList = commentsList + CommentEntity(
                                id = "c_${System.currentTimeMillis()}",
                                taskId = task.id,
                                content = newCommentText.trim(),
                                authorName = "You"
                            )
                            newCommentText = ""
                        }
                    },
                    enabled = newCommentText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = TasklyRed)
                }
            }
        }
    }

    // AI Auto-Breakdown Dialog
    if (showAiBreakdownDialog) {
        AlertDialog(
            onDismissRequest = { showAiBreakdownDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TasklyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Suggested Subtasks")
                }
            },
            text = {
                if (isBreakingDown) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = TasklyRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Decomposing task with Gemini AI...")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(suggestedSubtasks.size) { idx ->
                            val sub = suggestedSubtasks[idx]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { aiViewModel.toggleSubtaskSelection(idx) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = sub.isSelected,
                                    onCheckedChange = { aiViewModel.toggleSubtaskSelection(idx) },
                                    colors = CheckboxDefaults.colors(checkedColor = TasklyRed)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.title, style = MaterialTheme.typography.bodyMedium)
                                    Text("${sub.estimatedMinutes} mins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        aiViewModel.applySubtasks(task) {
                            showAiBreakdownDialog = false
                        }
                    },
                    enabled = !isBreakingDown && suggestedSubtasks.any { it.isSelected },
                    colors = ButtonDefaults.buttonColors(containerColor = TasklyRed)
                ) {
                    Text("Add Selected Subtasks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiBreakdownDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
