package com.example.data.repository

import com.example.data.local.TasklyDatabase
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LabelRepository(private val db: TasklyDatabase) {
    private val labelDao = db.labelDao()
    private val syncQueueDao = db.syncQueueDao()

    fun getAllLabels(): Flow<List<LabelEntity>> = labelDao.getAllLabels()

    suspend fun createLabel(name: String, colorHex: String = "#808080"): LabelEntity {
        val existing = labelDao.getLabelByName(name)
        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val label = LabelEntity(
            id = UUID.randomUUID().toString(),
            name = name.lowercase().removePrefix("@"),
            colorHex = colorHex,
            createdAt = now,
            updatedAt = now
        )
        labelDao.insertLabel(label)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "LABEL", entityId = label.id, operation = "UPSERT", timestamp = now)
        )
        return label
    }

    suspend fun deleteLabel(labelId: String) {
        val now = System.currentTimeMillis()
        labelDao.softDeleteLabel(labelId, now)
        syncQueueDao.enqueue(
            SyncQueueEntity(entityType = "LABEL", entityId = labelId, operation = "DELETE", timestamp = now)
        )
    }

    fun getLabelsForTask(taskId: String): Flow<List<LabelEntity>> = labelDao.getLabelsForTask(taskId)
}
