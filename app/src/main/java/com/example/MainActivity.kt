package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskEntity
import com.example.ui.components.ProjectTreeDrawer
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AiPlanDialog
import com.example.ui.screens.FilterAndSearchScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskDetailSheet
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.UpcomingScreen
import com.example.ui.theme.TasklyRed
import com.example.ui.theme.TasklyTheme
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()
    private val aiViewModel: AiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasklyTheme {
                TasklyApp(
                    taskViewModel = taskViewModel,
                    aiViewModel = aiViewModel
                )
            }
        }
    }
}

@Composable
fun TasklyApp(
    taskViewModel: TaskViewModel,
    aiViewModel: AiViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("taskly_prefs", Context.MODE_PRIVATE) }
    var isOnboardingCompleted by remember {
        mutableStateOf(prefs.getBoolean("onboarding_completed", false))
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onFinished = {
                prefs.edit().putBoolean("onboarding_completed", true).apply()
                isOnboardingCompleted = true
            }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation Destinations: 0 = Inbox, 1 = Today, 2 = Upcoming, 3 = Browse, 4 = AI Chat
    var currentNavTab by remember { mutableIntStateOf(1) } // Default to Today
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Dialogs & Sheets
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<TaskEntity?>(null) }
    var showAiPlanDialog by remember { mutableStateOf(false) }

    // Undo Snackbar handling
    val snackMessage by taskViewModel.snackMessage.collectAsState()
    LaunchedEffect(snackMessage) {
        snackMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg.contains("deleted", true) || msg.contains("completed", true)) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                taskViewModel.undoLastDelete()
            }
            taskViewModel.clearSnackMessage()
        }
    }

    // BackHandler routing
    BackHandler(enabled = drawerState.isOpen || selectedProjectId != null || isSettingsOpen || currentNavTab != 1) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            selectedProjectId != null -> selectedProjectId = null
            isSettingsOpen -> isSettingsOpen = false
            currentNavTab != 1 -> currentNavTab = 1
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProjectTreeDrawer(
                taskViewModel = taskViewModel,
                onSelectProject = { projId ->
                    selectedProjectId = projId
                },
                onOpenSettings = { isSettingsOpen = true },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (selectedProjectId == null && !isSettingsOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentNavTab == 0,
                            onClick = { currentNavTab = 0 },
                            icon = { Icon(Icons.Default.Inbox, contentDescription = "Inbox") },
                            label = { Text("Inbox") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TasklyRed.copy(alpha = 0.15f),
                                selectedIconColor = TasklyRed,
                                selectedTextColor = TasklyRed
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 1,
                            onClick = { currentNavTab = 1 },
                            icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                            label = { Text("Today") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TasklyRed.copy(alpha = 0.15f),
                                selectedIconColor = TasklyRed,
                                selectedTextColor = TasklyRed
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 2,
                            onClick = { currentNavTab = 2 },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Upcoming") },
                            label = { Text("Upcoming") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TasklyRed.copy(alpha = 0.15f),
                                selectedIconColor = TasklyRed,
                                selectedTextColor = TasklyRed
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 3,
                            onClick = { currentNavTab = 3 },
                            icon = { Icon(Icons.Default.FilterList, contentDescription = "Browse") },
                            label = { Text("Browse") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TasklyRed.copy(alpha = 0.15f),
                                selectedIconColor = TasklyRed,
                                selectedTextColor = TasklyRed
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 4,
                            onClick = { currentNavTab = 4 },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                            label = { Text("AI Chat") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TasklyRed.copy(alpha = 0.15f),
                                selectedIconColor = TasklyRed,
                                selectedTextColor = TasklyRed
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedProjectId == null && !isSettingsOpen && currentNavTab != 4) {
                    FloatingActionButton(
                        onClick = { showQuickAddSheet = true },
                        containerColor = TasklyRed,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp).testTag("main_add_task_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    isSettingsOpen -> {
                        SettingsScreen(
                            taskViewModel = taskViewModel,
                            onBack = { isSettingsOpen = false }
                        )
                    }
                    selectedProjectId != null -> {
                        ProjectDetailScreen(
                            projectId = selectedProjectId!!,
                            taskViewModel = taskViewModel,
                            onBack = { selectedProjectId = null },
                            onOpenTaskDetail = { selectedTaskForDetail = it },
                            onQuickAddTask = { showQuickAddSheet = true }
                        )
                    }
                    currentNavTab == 0 -> {
                        InboxScreen(
                            taskViewModel = taskViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenTaskDetail = { selectedTaskForDetail = it }
                        )
                    }
                    currentNavTab == 1 -> {
                        TodayScreen(
                            taskViewModel = taskViewModel,
                            aiViewModel = aiViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenTaskDetail = { selectedTaskForDetail = it },
                            onOpenAiPlan = { showAiPlanDialog = true }
                        )
                    }
                    currentNavTab == 2 -> {
                        UpcomingScreen(
                            taskViewModel = taskViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenTaskDetail = { selectedTaskForDetail = it }
                        )
                    }
                    currentNavTab == 3 -> {
                        FilterAndSearchScreen(
                            taskViewModel = taskViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenTaskDetail = { selectedTaskForDetail = it }
                        )
                    }
                    currentNavTab == 4 -> {
                        AiChatScreen(
                            taskViewModel = taskViewModel,
                            aiViewModel = aiViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }

    // Quick Add Natural Language Bottom Sheet
    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            taskViewModel = taskViewModel,
            aiViewModel = aiViewModel,
            onDismiss = { showQuickAddSheet = false }
        )
    }

    // Task Detail Sheet
    selectedTaskForDetail?.let { task ->
        TaskDetailSheet(
            task = task,
            taskViewModel = taskViewModel,
            aiViewModel = aiViewModel,
            onDismiss = { selectedTaskForDetail = null }
        )
    }

    // AI Daily Plan Dialog
    if (showAiPlanDialog) {
        AiPlanDialog(
            taskViewModel = taskViewModel,
            aiViewModel = aiViewModel,
            onDismiss = { showAiPlanDialog = false }
        )
    }
}
