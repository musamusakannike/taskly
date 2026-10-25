package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.TaskWithDetails
import com.example.ui.components.TaskItemRow
import com.example.ui.theme.Charcoal800
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    taskViewModel: TaskViewModel,
    onBack: () -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onQuickAddTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by taskViewModel.projects.collectAsState()
    val allTasks by taskViewModel.allTasksWithDetails.collectAsState()

    val currentProject = remember(projects, projectId) {
        projects.firstOrNull { it.id == projectId }
    }
    val projectTasks = remember(allTasks, projectId) {
        allTasks.filter { it.task.projectId == projectId && !it.task.isCompleted }
    }

    var isKanbanMode by remember { mutableStateOf(false) }

    val projectColor = try {
        Color(android.graphics.Color.parseColor(currentProject?.colorHex ?: "#DC4C3E"))
    } catch (_: Exception) {
        TasklyRed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(projectColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentProject?.name ?: "Project",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // View Toggle (List vs Kanban Board)
                    IconButton(
                        onClick = { isKanbanMode = !isKanbanMode },
                        modifier = Modifier.testTag("toggle_view_mode")
                    ) {
                        Icon(
                            imageVector = if (isKanbanMode) Icons.Default.ViewList else Icons.Default.ViewKanban,
                            contentDescription = if (isKanbanMode) "List View" else "Board View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onQuickAddTask(projectId) },
                containerColor = TasklyRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("project_add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isKanbanMode) {
            // Kanban Board View
            KanbanBoardView(
                tasks = projectTasks,
                onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                onOpenTaskDetail = onOpenTaskDetail,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // Standard List View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (projectTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks yet in this project. Tap '+' to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(projectTasks, key = { it.task.id }) { taskDetails ->
                        TaskItemRow(
                            taskDetails = taskDetails,
                            onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                            onClick = { onOpenTaskDetail(it) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanBoardView(
    tasks: List<TaskWithDetails>,
    onToggleCompletion: (TaskEntity) -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val urgentTasks = remember(tasks) { tasks.filter { it.task.priority in 1..2 } }
    val normalTasks = remember(tasks) { tasks.filter { it.task.priority in 3..4 } }

    val columns = listOf(
        "High Priority (P1/P2)" to urgentTasks,
        "Normal (P3/P4)" to normalTasks
    )

    LazyRow(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(columns) { (title, columnTasks) ->
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(bottom = 88.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${columnTasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(columnTasks, key = { it.task.id }) { taskDetails ->
                            TaskItemRow(
                                taskDetails = taskDetails,
                                onToggleCompletion = onToggleCompletion,
                                onClick = onOpenTaskDetail
                            )
                        }
                    }
                }
            }
        }
    }
}
