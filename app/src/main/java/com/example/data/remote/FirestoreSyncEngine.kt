package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.TasklyDatabase
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.model.SyncStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncEngine(
    private val context: Context,
    private val db: TasklyDatabase,
    private val scope: CoroutineScope
) {
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var projectListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null
    private var labelListener: ListenerRegistration? = null

    private fun getFirestore(): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
    }

    fun startListening() {
        val user = Firebase.auth.currentUser ?: return
        val firestore = getFirestore()
        val uid = user.uid

        // Projects listener
        projectListener?.remove()
        projectListener = firestore.collection("users").document(uid).collection("projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Sync", "Projects sync listener error: ${error.message}")
                    _syncStatus.value = SyncStatus.ERROR
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                        val id = doc.getString("id") ?: doc.id
                        val local = db.projectDao().getProjectByIdSync(id)
                        if (local == null || remoteUpdatedAt > local.updatedAt) {
                            val project = ProjectEntity(
                                id = id,
                                userId = uid,
                                parentId = doc.getString("parentId"),
                                name = doc.getString("name") ?: "Project",
                                colorHex = doc.getString("colorHex") ?: "#DC4C3E",
                                iconName = doc.getString("iconName") ?: "folder",
                                isFavorite = doc.getBoolean("isFavorite") ?: false,
                                isArchived = doc.getBoolean("isArchived") ?: false,
                                isInbox = doc.getBoolean("isInbox") ?: false,
                                sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0,
                                isDeleted = doc.getBoolean("isDeleted") ?: false,
                                updatedAt = remoteUpdatedAt,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                            db.projectDao().insertProject(project)
                        }
                    }
                    _syncStatus.value = SyncStatus.SYNCED
                }
            }

        // Tasks listener
        taskListener?.remove()
        taskListener = firestore.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Sync", "Tasks sync listener error: ${error.message}")
                    _syncStatus.value = SyncStatus.ERROR
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                        val id = doc.getString("id") ?: doc.id
                        val local = db.taskDao().getTaskByIdSync(id)
                        if (local == null || remoteUpdatedAt > local.updatedAt) {
                            val task = TaskEntity(
                                id = id,
                                userId = uid,
                                projectId = doc.getString("projectId") ?: "",
                                parentTaskId = doc.getString("parentTaskId"),
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                priority = doc.getLong("priority")?.toInt() ?: 4,
                                dueDate = doc.getLong("dueDate"),
                                dueTimeMinutes = doc.getLong("dueTimeMinutes")?.toInt(),
                                isAllDay = doc.getBoolean("isAllDay") ?: true,
                                recurrenceRule = doc.getString("recurrenceRule"),
                                isCompleted = doc.getBoolean("isCompleted") ?: false,
                                completedAt = doc.getLong("completedAt"),
                                estimatedMinutes = doc.getLong("estimatedMinutes")?.toInt(),
                                orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0,
                                isDeleted = doc.getBoolean("isDeleted") ?: false,
                                updatedAt = remoteUpdatedAt,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                            db.taskDao().insertTask(task)
                        }
                    }
                    _syncStatus.value = SyncStatus.SYNCED
                }
            }
    }

    fun stopListening() {
        projectListener?.remove()
        taskListener?.remove()
        labelListener?.remove()
    }

    fun syncNow() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }

        scope.launch(Dispatchers.IO) {
            _syncStatus.value = SyncStatus.SYNCING
            val firestore = getFirestore()
            val uid = user.uid

            try {
                // 1. Flush local queue to Firestore
                val queueItems = db.syncQueueDao().getQueueItems(50)
                for (item in queueItems) {
                    when (item.entityType) {
                        "TASK" -> {
                            val task = db.taskDao().getTaskByIdSync(item.entityId)
                            if (task != null) {
                                val docRef = firestore.collection("users").document(uid).collection("tasks").document(task.id)
                                val data = mapOf(
                                    "id" to task.id,
                                    "userId" to uid,
                                    "projectId" to task.projectId,
                                    "parentTaskId" to task.parentTaskId,
                                    "title" to task.title,
                                    "description" to task.description,
                                    "priority" to task.priority,
                                    "dueDate" to task.dueDate,
                                    "dueTimeMinutes" to task.dueTimeMinutes,
                                    "isAllDay" to task.isAllDay,
                                    "recurrenceRule" to task.recurrenceRule,
                                    "isCompleted" to task.isCompleted,
                                    "completedAt" to task.completedAt,
                                    "estimatedMinutes" to task.estimatedMinutes,
                                    "orderIndex" to task.orderIndex,
                                    "isDeleted" to task.isDeleted,
                                    "updatedAt" to task.updatedAt,
                                    "createdAt" to task.createdAt
                                )
                                docRef.set(data).await()
                            }
                        }
                        "PROJECT" -> {
                            val project = db.projectDao().getProjectByIdSync(item.entityId)
                            if (project != null) {
                                val docRef = firestore.collection("users").document(uid).collection("projects").document(project.id)
                                val data = mapOf(
                                    "id" to project.id,
                                    "userId" to uid,
                                    "parentId" to project.parentId,
                                    "name" to project.name,
                                    "colorHex" to project.colorHex,
                                    "iconName" to project.iconName,
                                    "isFavorite" to project.isFavorite,
                                    "isArchived" to project.isArchived,
                                    "isInbox" to project.isInbox,
                                    "sortOrder" to project.sortOrder,
                                    "isDeleted" to project.isDeleted,
                                    "updatedAt" to project.updatedAt,
                                    "createdAt" to project.createdAt
                                )
                                docRef.set(data).await()
                            }
                        }
                        "LABEL" -> {
                            val label = db.labelDao().getLabelById(item.entityId)
                            if (label != null) {
                                val docRef = firestore.collection("users").document(uid).collection("labels").document(label.id)
                                val data = mapOf(
                                    "id" to label.id,
                                    "userId" to uid,
                                    "name" to label.name,
                                    "colorHex" to label.colorHex,
                                    "isDeleted" to label.isDeleted,
                                    "updatedAt" to label.updatedAt,
                                    "createdAt" to label.createdAt
                                )
                                docRef.set(data).await()
                            }
                        }
                    }
                    db.syncQueueDao().dequeue(item.queueId)
                }

                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                Log.e("Sync", "Sync failed: ${e.message}", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }
}
