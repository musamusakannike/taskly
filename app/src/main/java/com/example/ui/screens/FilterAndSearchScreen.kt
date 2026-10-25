package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.local.entity.TaskEntity
import com.example.data.model.DueDateFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.Priority
import com.example.ui.components.TaskItemRow
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterAndSearchScreen(
    taskViewModel: TaskViewModel,
    onOpenDrawer: () -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Search, 1 = Filter Builder, 2 = Completed History
    val tabs = listOf("Search", "Filters", "Completed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse & Filter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TasklyRed
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) TasklyRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> SearchTabContent(taskViewModel = taskViewModel, onOpenTaskDetail = onOpenTaskDetail)
                1 -> FilterBuilderTabContent(taskViewModel = taskViewModel, onOpenTaskDetail = onOpenTaskDetail)
                2 -> CompletedHistoryTabContent(taskViewModel = taskViewModel, onOpenTaskDetail = onOpenTaskDetail)
            }
        }
    }
}

@Composable
private fun SearchTabContent(
    taskViewModel: TaskViewModel,
    onOpenTaskDetail: (TaskEntity) -> Unit
) {
    val query by taskViewModel.searchQuery.collectAsState()
    val results by taskViewModel.searchResults.collectAsState()
    val allTasks by taskViewModel.allTasksWithDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { taskViewModel.searchQuery.value = it },
            placeholder = { Text("Search tasks, projects, notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { taskViewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("search_text_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TasklyRed)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Type any keyword to search across all tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val taskDetailsMap = remember(allTasks) { allTasks.associateBy { it.task.id } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(results, key = { it.id }) { task ->
                    val details = taskDetailsMap[task.id] ?: com.example.data.model.TaskWithDetails(task = task)
                    TaskItemRow(
                        taskDetails = details,
                        onToggleCompletion = { taskViewModel.toggleTaskCompletion(it) },
                        onClick = { onOpenTaskDetail(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBuilderTabContent(
    taskViewModel: TaskViewModel,
    onOpenTaskDetail: (TaskEntity) -> Unit
) {
    val allTasks by taskViewModel.allTasksWithDetails.collectAsState()
    val labels by taskViewModel.labels.collectAsState()
    val projects by taskViewModel.projects.collectAsState()

    var selectedPriority by remember { mutableStateOf<Int?>(null) }
    var selectedDueDate by remember { mutableStateOf(DueDateFilter.ALL) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val filtered = remember(allTasks, selectedPriority, selectedDueDate, selectedProjectId) {
        allTasks.filter { details ->
            val task = details.task
            if (task.isCompleted) return@filter false
            if (selectedPriority != null && task.priority != selectedPriority) return@filter false
            if (selectedProjectId != null && task.projectId != selectedProjectId) return@filter false
            when (selectedDueDate) {
                DueDateFilter.ALL -> true
                DueDateFilter.OVERDUE -> task.dueDate != null && task.dueDate < todayStart
                DueDateFilter.TODAY -> task.dueDate != null && task.dueDate >= todayStart && task.dueDate < (todayStart + 86400000L)
                DueDateFilter.THIS_WEEK -> task.dueDate != null && task.dueDate >= todayStart && task.dueDate <= (todayStart + 7 * 86400000L)
                DueDateFilter.NO_DATE -> task.dueDate == null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Filter by Priority:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "All", 1 to "P1", 2 to "P2", 3 to "P3", 4 to "P4").forEach { (lvl, label) ->
                val isSel = selectedPriority == lvl
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) TasklyRed else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedPriority = lvl }
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Filter by Due Date:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                DueDateFilter.ALL to "Any Date",
                DueDateFilter.OVERDUE to "Overdue",
                DueDateFilter.TODAY to "Due Today",
                DueDateFilter.THIS_WEEK to "This Week"
            ).forEach { (filter, label) ->
                val isSel = selectedDueDate == filter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) TasklyRed else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedDueDate = filter }
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Matching Tasks (${filtered.size}):",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(filtered, key = { it.task.id }) { taskDetails ->
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

@Composable
private fun CompletedHistoryTabContent(
    taskViewModel: TaskViewModel,
    onOpenTaskDetail: (TaskEntity) -> Unit
) {
    val completedTasks by taskViewModel.completedTasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Completed Archive (${completedTasks.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(completedTasks, key = { it.task.id }) { taskDetails ->
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
