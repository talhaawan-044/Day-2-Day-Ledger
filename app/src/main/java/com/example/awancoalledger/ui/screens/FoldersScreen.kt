package com.example.awancoalledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Folder
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.PrimaryBlue
import com.example.awancoalledger.viewmodel.LedgerViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    viewModel: LedgerViewModel,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToEditor: (Int?) -> Unit
) {
    val folders by viewModel.allFolders.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(if (isEditMode) "Done" else "Edit", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Folders",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.EditNote, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                FolderItem(
                    title = "All Notes",
                    icon = Icons.Default.Description,
                    count = allNotes.size,
                    showChevron = !isEditMode,
                    onClick = { if (!isEditMode) onNavigateToFolder("all") }
                )
            }
            
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    "FOLDERS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            
            if (folders.isEmpty()) {
                item {
                    Text(
                        "No folders yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(folders) { folder ->
                    val noteCount = allNotes.count { it.folderId == folder.id }
                    FolderItem(
                        title = folder.name,
                        icon = Icons.Default.Folder,
                        count = noteCount,
                        isEditMode = isEditMode,
                        onRename = { folderToRename = folder },
                        onDelete = { folderToDelete = folder },
                        onClick = { if (!isEditMode) onNavigateToFolder(folder.id.toString()) }
                    )
                }
            }
        }
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        FolderDialog(
            title = "New Folder",
            initialName = "",
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { name ->
                viewModel.addFolder(name)
                showAddFolderDialog = false
            }
        )
    }

    // Rename Folder Dialog
    folderToRename?.let { folder ->
        FolderDialog(
            title = "Rename Folder",
            initialName = folder.name,
            onDismiss = { folderToRename = null },
            onConfirm = { name ->
                viewModel.updateFolder(folder.copy(name = name))
                folderToRename = null
            }
        )
    }

    // Delete Folder Confirmation
    folderToDelete?.let { folder ->
        DeleteConfirmationDialog(
            title = "Delete Folder",
            message = "Are you sure you want to delete \"${folder.name}\"? Notes inside this folder will not be deleted.",
            onConfirm = {
                viewModel.deleteFolder(folder)
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isEditMode: Boolean = false,
    showChevron: Boolean = true,
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (title != "All Notes") onDelete() }
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode && title != "All Notes") {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.RemoveCircle, null, tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(12.dp))
            }
            
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, modifier = Modifier.weight(1f))
            
            if (isEditMode && title != "All Notes") {
                IconButton(onClick = onRename, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                if (showChevron) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun FolderDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            PremiumInput(
                label = "Folder Name",
                value = name,
                onValueChange = { name = it }
            )

        },
        buttons = {
            IOSDialogButton(
                text = "Cancel",
                onClick = onDismiss
            )
            IOSDialogButton(
                text = "Save",
                fontWeight = FontWeight.Bold,
                isLast = true,
                onClick = {
                    if (name.isNotEmpty()) onConfirm(name)
                }
            )
        }
    )
}
