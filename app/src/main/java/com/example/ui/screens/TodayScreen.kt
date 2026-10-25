package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TaskEntity
import com.example.data.model.TaskWithDetails
import com.example.ui.components.TaskItemRow
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    taskViewModel: TaskViewModel,
    aiViewModel: AiViewModel,
    onOpenDrawer: () -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onOpenAiPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayTasks by taskViewModel.todayTasks.collectAsState()
    val overdueTasks by taskViewModel.overdueTasks.collectAsState()

    var overdueExpanded by remember { mutableStateOf(true) }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("drawer_menu_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Smart Schedule / "Plan My Day" button
                    Button(
                        onClick = onOpenAiPlan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TasklyRed.copy(alpha = 0.12f),
                            contentColor = TasklyRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("plan_my_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Plan My Day",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Plan My Day",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Overdue Tasks Section (if any)
            if (overdueTasks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = PriorityP1.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { overdueExpanded = !overdueExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Overdue",
                                        tint = PriorityP1,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Overdue (${overdueTasks.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PriorityP1
                                    )
                                }
                                Icon(
                                    imageVector = if (overdueExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = PriorityP1
                                )
                            }

                            AnimatedVisibility(visible = overdueExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    overdueTasks.forEach { taskDetails ->
                                        TaskItemRow(
                                            taskDetails = taskDetails,
                                            onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                                            onClick = { onOpenTaskDetail(it) },
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Reschedule all overdue to today
                                    Button(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            cal.set(Calendar.HOUR_OF_DAY, 0)
                                            cal.set(Calendar.MINUTE, 0)
                                            cal.set(Calendar.SECOND, 0)
                                            cal.set(Calendar.MILLISECOND, 0)
                                            val today = cal.timeInMillis
                                            overdueTasks.forEach { taskViewModel.rescheduleTask(it.task.id, today, null) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PriorityP1),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text("Reschedule all to Today", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Today's Tasks
            if (todayTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Scheduled Tasks",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(todayTasks, key = { it.task.id }) { taskDetails ->
                    TaskItemRow(
                        taskDetails = taskDetails,
                        onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                        onClick = { onOpenTaskDetail(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else if (overdueTasks.isEmpty()) {
                // Empty state
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(TasklyRed.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TasklyRed,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All clear for today!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+' below to capture a new task or take a rest.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
