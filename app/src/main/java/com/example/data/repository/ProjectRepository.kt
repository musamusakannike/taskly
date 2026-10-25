package com.example.data.repository

import com.example.data.local.TasklyDatabase
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.model.ProjectTreeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class ProjectRepository(private val db: TasklyDatabase) {
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val syncQueueDao = db.syncQueueDao()

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllActiveProjects()

    fun getInboxProject(): Flow<ProjectEntity?> = projectDao.getInboxProject()

    fun getProjectTree(): Flow<List<ProjectTreeItem>> {
        return combine(
            projectDao.getAllActiveProjects(),
            taskDao.getAllTasks()
        ) { projects, tasks ->
            val tasksByProject = tasks.filter { !it.isCompleted }.groupBy { it.projectId }
            val childrenByParent = projects.groupBy { it.parentId }

            val tree = mutableListOf<ProjectTreeItem>()

            fun traverse(parentId: String?, depth: Int) {
                val children = childrenByParent[parentId] ?: return
                for (child in children) {
                    val count = tasksByProject[child.id]?.size ?: 0
                    val hasKids = childrenByParent[child.id]?.isNotEmpty() == true
                    tree.add(
                        ProjectTreeItem(
                            project = child,
                            depth = depth,
                            taskCount = count,
                            hasChildren = hasKids
                        )
                    )
                    traverse(child.id, depth + 1)
                }
            }

            // Top-level projects
            traverse(null, 0)
            tree
        }
    }

    suspend fun createProject(
        name: String,
        parentId: String? = null,
        colorHex: String = "#DC4C3E",
        isFavorite: Boolean = false
    ): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            parentId = parentId,
            name = name,
            colorHex = colorHex,
            isFavorite = isFavorite,
            createdAt = now,
            updatedAt = now
        )
        projectDao.insertProject(project)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "PROJECT", entityId = project.id, operation = "UPSERT", timestamp = now)
        )
        return project
    }

    suspend fun updateProject(project: ProjectEntity) {
        val now = System.currentTimeMillis()
        val updated = project.copy(updatedAt = now)
        projectDao.updateProject(updated)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "PROJECT", entityId = updated.id, operation = "UPSERT", timestamp = now)
        )
    }

    suspend fun toggleFavorite(projectId: String, isFavorite: Boolean) {
        val now = System.currentTimeMillis()
        projectDao.updateFavorite(projectId, isFavorite, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "PROJECT", entityId = projectId, operation = "UPSERT", timestamp = now)
        )
    }

    suspend fun toggleArchive(projectId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        projectDao.updateArchived(projectId, isArchived, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "PROJECT", entityId = projectId, operation = "UPSERT", timestamp = now)
        )
    }

    /**
     * Delete project with options:
     * - moveContentsToParent: if true, moves subprojects and tasks to parent/inbox; if false, deletes them recursively
     */
    suspend fun deleteProject(project: ProjectEntity, moveContentsToParent: Boolean) {
        val now = System.currentTimeMillis()
        if (moveContentsToParent) {
            val targetParentId = project.parentId
            val inbox = projectDao.getInboxProjectSync()
            val fallbackProjectId = targetParentId ?: inbox?.id ?: "inbox"

            // Move subprojects to parent
            projectDao.moveChildren(project.id, targetParentId, now)
            // Move tasks to target parent or inbox
            taskDao.moveAllTasksToProject(project.id, fallbackProjectId, now)
        } else {
            // Delete subprojects and tasks recursively
            projectDao.softDeleteChildren(project.id, now)
            taskDao.softDeleteTasksInProject(project.id, now)
        }

        projectDao.softDeleteProject(project.id, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "PROJECT", entityId = project.id, operation = "DELETE", timestamp = now)
        )
    }
}
