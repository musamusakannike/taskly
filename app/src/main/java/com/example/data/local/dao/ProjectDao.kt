package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllActiveProjectsSync(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id AND isDeleted = 0 LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdSync(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isInbox = 1 AND isDeleted = 0 LIMIT 1")
    fun getInboxProject(): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE isInbox = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getInboxProjectSync(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE parentId = :parentId AND isDeleted = 0 ORDER BY sortOrder ASC")
    fun getChildProjects(parentId: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteProject(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isDeleted = 1, updatedAt = :timestamp WHERE parentId = :parentId")
    suspend fun softDeleteChildren(parentId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET parentId = :newParentId, updatedAt = :timestamp WHERE parentId = :oldParentId")
    suspend fun moveChildren(oldParentId: String, newParentId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateArchived(id: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())
}
