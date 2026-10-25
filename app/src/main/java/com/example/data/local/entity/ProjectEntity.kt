package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["parentId"]),
        Index(value = ["isInbox"])
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val parentId: String? = null,
    val name: String,
    val colorHex: String = "#DC4C3E",
    val iconName: String = "folder",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isInbox: Boolean = false,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
