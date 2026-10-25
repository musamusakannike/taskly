package com.example

import android.app.Application
import com.example.data.local.DatabaseSeeder
import com.example.data.local.TasklyDatabase
import com.example.data.remote.AuthManager
import com.example.data.remote.FirestoreSyncEngine
import com.example.data.repository.LabelRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.TaskRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TasklyApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: TasklyDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var labelRepository: LabelRepository
        private set

    lateinit var syncEngine: FirestoreSyncEngine
        private set

    lateinit var authManager: AuthManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 0. Firebase initialization check
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {}

        // 1. Notification channels
        NotificationHelper.createChannels(this)

        // 2. Room database & repositories
        database = TasklyDatabase.getInstance(this)
        taskRepository = TaskRepository(database)
        projectRepository = ProjectRepository(database)
        labelRepository = LabelRepository(database)

        // 3. Auth & Sync
        authManager = AuthManager(this, applicationScope)
        syncEngine = FirestoreSyncEngine(this, database, applicationScope)

        // 4. Seed initial data
        applicationScope.launch {
            DatabaseSeeder.seedIfNeeded(database)
        }
    }
}
