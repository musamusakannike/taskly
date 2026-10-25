package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.ProjectTreeItem
import com.example.data.model.SyncStatus
import com.example.ui.theme.Charcoal800
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TasklyRed
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun ProjectTreeDrawer(
    taskViewModel: TaskViewModel,
    onSelectProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectTree by taskViewModel.projectTree.collectAsState()
    val syncStatus by taskViewModel.syncStatus.collectAsState()
    val currentUser by taskViewModel.currentUser.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var parentIdForNewProject by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        ) {
            // Header: App Logo + Sync Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TasklyRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Taskly",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUser?.email ?: "Offline mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sync status icon
                IconButton(
                    onClick = { taskViewModel.syncNow() },
                    modifier = Modifier.testTag("sync_now_button")
                ) {
                    when (syncStatus) {
                        SyncStatus.SYNCED -> Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = SuccessGreen)
                        SyncStatus.SYNCING -> Icon(Icons.Default.CloudSync, contentDescription = "Syncing", tint = TasklyRed)
                        SyncStatus.OFFLINE -> Icon(Icons.Default.CloudOff, contentDescription = "Offline", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        SyncStatus.ERROR -> Icon(Icons.Default.ErrorOutline, contentDescription = "Sync Error", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Projects Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROJECTS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        parentIdForNewProject = null
                        showCreateDialog = true
                    },
                    modifier = Modifier.size(32.dp).testTag("add_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Project",
                        tint = TasklyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tree list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(projectTree, key = { it.project.id }) { treeItem ->
                    ProjectTreeRow(
                        item = treeItem,
                        onSelect = {
                            onSelectProject(treeItem.project.id)
                            onCloseDrawer()
                        },
                        onAddSubproject = {
                            parentIdForNewProject = treeItem.project.id
                            showCreateDialog = true
                        },
                        onToggleFavorite = { taskViewModel.toggleFavorite(treeItem.project) },
                        onToggleArchive = { taskViewModel.toggleArchive(treeItem.project) },
                        onDelete = { projectToDelete = treeItem.project }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Footer: Settings button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenSettings()
                        onCloseDrawer()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings & Account",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            parentId = parentIdForNewProject,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorHex, isFav ->
                taskViewModel.createProject(name, parentIdForNewProject, colorHex, isFav)
                showCreateDialog = false
            }
        )
    }

    // Delete Project Dialog: "Delete folder: asks whether to delete or move contents"
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete '${proj.name}'?") },
            text = {
                Text("This folder contains tasks and sub-projects. Would you like to move its contents to the parent folder, or delete everything inside?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        taskViewModel.deleteProject(proj, moveContentsToParent = false)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        taskViewModel.deleteProject(proj, moveContentsToParent = true)
                        projectToDelete = null
                    }
                ) {
                    Text("Move Contents to Parent")
                }
            }
        )
    }
}

@Composable
private fun ProjectTreeRow(
    item: ProjectTreeItem,
    onSelect: () -> Unit,
    onAddSubproject: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val project = item.project
    val indentDp = (item.depth * 20).dp
    var menuExpanded by remember { mutableStateOf(false) }

    val projectColor = try {
        Color(android.graphics.Color.parseColor(project.colorHex))
    } catch (_: Exception) {
        TasklyRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 16.dp + indentDp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon / Bullet
        if (project.isInbox) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = "Inbox",
                tint = TasklyRed,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(projectColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = project.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (project.isInbox || item.depth == 0) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Task count badge
        if (item.taskCount > 0) {
            Text(
                text = "${item.taskCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // Actions Menu (for non-Inbox)
        if (!project.isInbox) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Sub-Project") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddSubproject()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (project.isFavorite) "Unfavourite" else "Favourite") },
                        leadingIcon = {
                            Icon(
                                if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (project.isFavorite) TasklyRed else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (project.isArchived) "Unarchive" else "Archive") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onToggleArchive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    parentId: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, isFav: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#DC4C3E") }
    var isFavorite by remember { mutableStateOf(false) }

    val colors = listOf("#DC4C3E", "#EB8909", "#246FE0", "#058527", "#14AAF5", "#96C3EB", "#B8256F", "#808080")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (parentId != null) "New Sub-Project" else "New Project") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Colour Code:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), selectedColor, isFavorite)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
