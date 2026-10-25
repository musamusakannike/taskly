package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labels",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["name"])
    ]
)
data class LabelEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val name: String,
    val colorHex: String = "#808080",
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_labels",
    primaryKeys = ["taskId", "labelId"],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["labelId"])
    ]
)
data class TaskLabelCrossRef(
    val taskId: String,
    val labelId: String
)
