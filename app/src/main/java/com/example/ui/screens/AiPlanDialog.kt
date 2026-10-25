package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun AiPlanDialog(
    taskViewModel: TaskViewModel,
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    val todayTasks by taskViewModel.todayTasks.collectAsState()
    val overdueTasks by taskViewModel.overdueTasks.collectAsState()

    val isPlanning by aiViewModel.isPlanning.collectAsState()
    val planResult by aiViewModel.dailyPlanResult.collectAsState()
    val error by aiViewModel.planningError.collectAsState()

    LaunchedEffect(Unit) {
        if (planResult == null && !isPlanning) {
            aiViewModel.generateDailyPlan(
                todayTasks.map { it.task },
                overdueTasks.map { it.task }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TasklyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart Daily Plan")
            }
        },
        text = {
            if (isPlanning) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                ) {
                    CircularProgressIndicator(color = TasklyRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing priorities & creating time blocks...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (error != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Unable to generate plan: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            aiViewModel.generateDailyPlan(
                                todayTasks.map { it.task },
                                overdueTasks.map { it.task }
                            )
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else if (planResult != null) {
                val plan = planResult!!
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        // Motivational quote
                        Text(
                            text = "\"${plan.motivationalQuote}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TasklyRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TasklyRed.copy(alpha = 0.08f))
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Proposed Schedule Time-Blocks:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(plan.timeBlocks) { block ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TasklyRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TasklyRed, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = block.timeSlot,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TasklyRed
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(block.taskTitle, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text(block.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Reschedule Suggestions
                    if (plan.rescheduleSuggestions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Suggested Rescheduling (Avoid Burnout):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PriorityP2
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        items(plan.rescheduleSuggestions) { sug ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PriorityP2.copy(alpha = 0.08f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sug.taskTitle, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Move to ${sug.suggestedDate}: ${sug.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    aiViewModel.applyRescheduleSuggestions {
                        onDismiss()
                    }
                },
                enabled = planResult != null && !isPlanning,
                colors = ButtonDefaults.buttonColors(containerColor = TasklyRed),
                modifier = Modifier.testTag("accept_plan_button")
            ) {
                Text("Accept Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
