package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    taskViewModel: TaskViewModel,
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    val rawInput by aiViewModel.rawInput.collectAsState()
    val draft by aiViewModel.liveDraft.collectAsState()
    val projects by taskViewModel.projects.collectAsState()

    var isDeepParsing by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var dateMenuExpanded by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    // Match draft project name to existing project
    LaunchedEffect(draft.projectName, projects) {
        if (!draft.projectName.isNullOrBlank()) {
            val matched = projects.firstOrNull { it.name.equals(draft.projectName, ignoreCase = true) }
            if (matched != null) {
                selectedProjectId = matched.id
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
            Text(
                text = "Quick Task Capture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type naturally with dates, #project, @label, p1-p4",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input field
            OutlinedTextField(
                value = rawInput,
                onValueChange = { aiViewModel.onInputChanged(it) },
                placeholder = {
                    Text(
                        text = "e.g. Call client tomorrow 3pm #Work p1 @urgent",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("quick_add_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TasklyRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Preview Token Chip Row
            Text(
                text = "Live Parser Preview:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority Chip
                val priorityColor = when (draft.priority) {
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
                            .background(priorityColor.copy(alpha = 0.15f))
                            .clickable { priorityMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Priority",
                            tint = priorityColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "P${draft.priority}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = priorityColor
                        )
                    }

                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false }
                    ) {
                        listOf(1 to PriorityP1, 2 to PriorityP2, 3 to PriorityP3, 4 to PriorityP4).forEach { (p, col) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = col, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("P$p Priority", color = col, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    aiViewModel.onInputChanged("${rawInput.replace(Regex("(?i)\\bp[1-4]\\b"), "")} p$p".trim())
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Chip
                val dateLabel = when (val due = draft.dueDateEpochMillis) {
                    null -> "No Date"
                    todayStart -> "Today"
                    todayStart + 86400000L -> "Tomorrow"
                    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(due))
                }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (draft.dueDateEpochMillis != null) TasklyRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { dateMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Due Date",
                            tint = if (draft.dueDateEpochMillis != null) TasklyRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (draft.dueDateEpochMillis != null) TasklyRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = dateMenuExpanded,
                        onDismissRequest = { dateMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Today") },
                            onClick = {
                                aiViewModel.onInputChanged("$rawInput today")
                                dateMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tomorrow") },
                            onClick = {
                                aiViewModel.onInputChanged("$rawInput tomorrow")
                                dateMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Next week") },
                            onClick = {
                                aiViewModel.onInputChanged("$rawInput next week")
                                dateMenuExpanded = false
                            }
                        )
                    }
                }

                // Time Chip
                if (draft.dueTimeMinutes != null) {
                    val h = draft.dueTimeMinutes!! / 60
                    val m = draft.dueTimeMinutes!! % 60
                    val isPm = h >= 12
                    val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
                    val timeStr = String.format(Locale.getDefault(), "%d:%02d %s", displayH, m, if (isPm) "PM" else "AM")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Due Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Project Chip
                val targetProj = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull { it.name.equals(draft.projectName, true) }
                val projName = targetProj?.name ?: draft.projectName ?: "Inbox"
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { projectMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Project",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "#$projName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = projectMenuExpanded,
                        onDismissRequest = { projectMenuExpanded = false }
                    ) {
                        projects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.name) },
                                onClick = {
                                    selectedProjectId = proj.id
                                    projectMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Labels
                draft.labelNames.forEach { lbl ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "@$lbl",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Recurrence
                if (!draft.recurrenceRule.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = draft.recurrenceRule!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row: AI Deep Parse button + Save Task Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Deep Parse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TasklyRed.copy(alpha = 0.08f))
                        .clickable(enabled = rawInput.isNotBlank() && !isDeepParsing) {
                            isDeepParsing = true
                            aiViewModel.parseDeepWithGemini {
                                isDeepParsing = false
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isDeepParsing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TasklyRed)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Parse",
                            tint = TasklyRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDeepParsing) "Parsing with AI..." else "Deep AI Parse",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TasklyRed
                    )
                }

                // Save button
                IconButton(
                    onClick = {
                        val titleToSave = draft.cleanTitle.ifBlank { rawInput.trim() }
                        if (titleToSave.isNotBlank()) {
                            taskViewModel.createTask(
                                title = titleToSave,
                                priority = draft.priority,
                                dueDate = draft.dueDateEpochMillis,
                                dueTimeMinutes = draft.dueTimeMinutes,
                                recurrenceRule = draft.recurrenceRule,
                                projectId = selectedProjectId
                            )
                            aiViewModel.onInputChanged("")
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("save_task_button")
                        .clip(CircleShape)
                        .background(TasklyRed),
                    enabled = rawInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Save Task",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
