package com.example.awancoalledger.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Note
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.viewmodel.LedgerViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

enum class NoteSortOrder(val label: String) {
    DATE_MODIFIED("Date Modified"),
    DATE_CREATED("Date Created"),
    TITLE_AZ("Title A → Z"),
    TITLE_ZA("Title Z → A")
}

/** iOS-style smart date: time today, "Yesterday", weekday, MMM d, or MM/dd/yy */
fun formatSmartDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val diffMs = now.timeInMillis - timestamp
    val diffDays = (diffMs / (1000L * 60 * 60 * 24)).toInt()
    return when {
        now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        diffDays == 1 -> "Yesterday"
        diffDays < 7 && now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
        viewModel: LedgerViewModel,
        folderId: Int?,
        onNavigateToEditor: (Int?) -> Unit,
        onBack: () -> Unit
) {
    val notes by viewModel.allNotes.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val currentFolder = remember(folderId, folders) { folders.find { it.id == folderId } }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    val isGridView by viewModel.isGridView.collectAsState()
    var sortOrder by remember { mutableStateOf(NoteSortOrder.DATE_MODIFIED) }
    var showSortMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var contextMenuNote by remember { mutableStateOf<Note?>(null) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val filteredNotes =
            remember(notes, folderId, searchQuery, sortOrder) {
                notes
                        .filter { note ->
                            (folderId == null || note.folderId == folderId) &&
                                    (searchQuery.isEmpty() ||
                                            note.title.contains(searchQuery, ignoreCase = true) ||
                                            note.content.contains(searchQuery, ignoreCase = true))
                        }
                        .let { list ->
                            when (sortOrder) {
                                NoteSortOrder.DATE_MODIFIED -> list.sortedByDescending { it.date }
                                NoteSortOrder.DATE_CREATED -> list.sortedByDescending { it.id }
                                NoteSortOrder.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
                                NoteSortOrder.TITLE_ZA ->
                                        list.sortedByDescending { it.title.lowercase() }
                            }
                        }
            }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val otherNotes = filteredNotes.filter { !it.isPinned }

    Scaffold(
            containerColor = MaterialTheme.colorScheme.background,

            bottomBar = {
                BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(80.dp).navigationBarsPadding()
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showAddFolderDialog = true 
                        }) {
                            Icon(
                                Icons.Outlined.CreateNewFolder,
                                contentDescription = "New Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        AnimatedContent(targetState = filteredNotes.size, label = "bottomCount") {
                                count ->
                            Text(
                                    if (count == 1) "1 Note" else "$count Notes",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onNavigateToEditor(null)
                        }) {
                            Icon(
                                    Icons.Outlined.EditNote,
                                    contentDescription = "New Note",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
    ) { padding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp)
                ) {
                    com.example.awancoalledger.ui.components.ScreenHeader(
                        title = currentFolder?.name ?: "All Notes",
                        subtitle = "${filteredNotes.size} notes",
                        onBack = { onBack() },
                        actions = {
                            Box {
                                IconButton(onClick = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showSortMenu = true 
                                }) {
                                    Icon(Icons.Outlined.Sort, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    NoteSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                                text = {
                                                    Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (sortOrder == order) {
                                                            Icon(
                                                                    Icons.Outlined.Check,
                                                                    null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                        } else {
                                                            Spacer(Modifier.width(24.dp))
                                                        }
                                                        Text(
                                                                order.label,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 15.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    sortOrder = order
                                                    showSortMenu = false
                                                }
                                        )
                                    }
                                }
                            }
                            // Grid / List toggle
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.toggleGridView(!isGridView) 
                            }) {
                                Crossfade(
                                        targetState = isGridView,
                                        animationSpec = tween(200),
                                        label = "viewToggle"
                                ) { grid ->
                                    Icon(
                                            if (grid) Icons.AutoMirrored.Outlined.List
                                            else Icons.Outlined.GridView,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )

                    // ── iOS-style search bar with animated cancel ─────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumInput(
                            label = "Search",
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isNotEmpty()) isSearchFocused = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Cancel, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AnimatedVisibility(
                            visible = isSearchFocused || searchQuery.isNotEmpty(),
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchFocused = false
                                }
                            ) { Text("Cancel", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) }
                        }
                    }

                }
            }
            if (filteredNotes.isEmpty()) {
                item {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 60.dp)
                    ) {
                        Icon(
                                if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff
                                else Icons.Outlined.NoteAlt,
                                null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                                if (searchQuery.isNotEmpty()) "No Results" else "No Notes Yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                                if (searchQuery.isNotEmpty()) "No notes match \"$searchQuery\"."
                                else "Create your first note.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Create Your First Note",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToEditor(null) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                if (pinnedNotes.isNotEmpty()) {
                    item { Box(modifier = Modifier.padding(horizontal = 16.dp)) { SectionHeader("PINNED", pinnedNotes.size) } }
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AnimatedContent(isGridView, label = "pinnedView") { grid ->
                                if (grid) {
                                    NoteGridGroup(
                                            notes = pinnedNotes,
                                            onNoteClick = onNavigateToEditor,
                                            onDelete = { noteToDelete = it },
                                            onLongPress = { contextMenuNote = it }
                                    )
                                } else {
                                    NoteListGroup(
                                            notes = pinnedNotes,
                                            onNoteClick = onNavigateToEditor,
                                            onDelete = { noteToDelete = it },
                                            onLongPress = { contextMenuNote = it }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (otherNotes.isNotEmpty()) {
                    item { Box(modifier = Modifier.padding(horizontal = 16.dp)) { SectionHeader(if (pinnedNotes.isEmpty()) "NOTES" else "OTHERS", otherNotes.size) } }
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AnimatedContent(isGridView, label = "othersView") { grid ->
                                if (grid) {
                                    NoteGridGroup(
                                            notes = otherNotes,
                                            onNoteClick = onNavigateToEditor,
                                            onDelete = { noteToDelete = it },
                                            onLongPress = { contextMenuNote = it }
                                    )
                                } else {
                                    NoteListGroup(
                                            notes = otherNotes,
                                            onNoteClick = onNavigateToEditor,
                                            onDelete = { noteToDelete = it },
                                            onLongPress = { contextMenuNote = it }
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    // ── Context menu (long-press) ─────────────────────────────────────────────
    contextMenuNote?.let { note ->
        NoteContextMenu(
                note = note,
                viewModel = viewModel,
                onDismiss = { contextMenuNote = null },
                onDelete = {
                    noteToDelete = note
                    contextMenuNote = null
                },
                onOpen = {
                    onNavigateToEditor(note.id)
                    contextMenuNote = null
                }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    noteToDelete?.let { note ->
        DeleteConfirmationDialog(
                title = "Delete Note",
                message =
                        "Are you sure you want to delete \"${note.title.ifEmpty { "New Note" }}\"?",
                onConfirm = {
                    viewModel.deleteNote(note)
                    noteToDelete = null
                },
                onDismiss = { noteToDelete = null }
        )
    }

    if (showAddFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("New Folder", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter a name for this folder.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    PremiumInput(
                        label = "Name",
                        value = folderName,
                        onValueChange = { folderName = it }
                    )

                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.addFolder(folderName)
                            showAddFolderDialog = false
                        }
                    },
                    enabled = folderName.isNotBlank()
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(label: String, count: Int) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(6.dp))
        Text("($count)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 0.5.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Context Menu (iOS-style bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteContextMenu(
        note: Note,
        viewModel: LedgerViewModel,
        onDismiss: () -> Unit,
        onDelete: () -> Unit,
        onOpen: () -> Unit
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                        modifier =
                                Modifier.padding(top = 12.dp)
                                        .size(width = 36.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outline)
                )
            }
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 36.dp)
        ) {
            // Note preview header
            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                        modifier =
                                Modifier.size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                                note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            note.title.firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                            color = note.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            note.title.ifEmpty { "New Note" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(formatSmartDate(note.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                if (note.isPinned) {
                    Icon(
                            Icons.Outlined.PushPin,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            ContextMenuRow(
                    icon = Icons.Outlined.OpenInNew,
                    label = "Open Note",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onOpen
            )
            ContextMenuRow(
                    icon = if (note.isPinned) Icons.Outlined.PushPin else Icons.Outlined.PushPin,
                    label = if (note.isPinned) "Unpin Note" else "Pin to Top",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.updateNote(note.copy(isPinned = !note.isPinned))
                        onDismiss()
                    }
            )
            ContextMenuRow(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Duplicate Note",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.addNote(
                                title = note.title.ifEmpty { "New Note" } + " (Copy)",
                                content = note.content,
                                color = note.color,
                                textColor = note.textColor,
                                fontSize = note.fontSize,
                                isPinned = false,
                                folderId = note.folderId
                        )
                        onDismiss()
                    }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            ContextMenuRow(
                    icon = Icons.Outlined.Delete,
                    label = "Delete Note",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
            )
        }
    }
}

@Composable
private fun ContextMenuRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// List View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteListGroup(
        notes: List<Note>,
        onNoteClick: (Int) -> Unit,
        onDelete: (Note) -> Unit,
        onLongPress: (Note) -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            notes.forEachIndexed { index, note ->
                SwipeableItem(
                    onEdit = { onNoteClick(note.id) }, onDelete = { onDelete(note) },
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    content = {
                        NoteListItem(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onLongPress = { onLongPress(note) }
                        )
                    },
                )
                if (index < notes.size - 1) {
                    HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(note: Note, onClick: () -> Unit, onLongPress: () -> Unit) {
    val fontColor = note.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val noteColor = note.color?.let { Color(it) }

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                            .background(noteColor ?: Color.Transparent)
    ) {
        if (note.bgImageId != null && note.bgImageId != NoteBackgrounds.NONE) {
            NoteBackgroundRenderer(bgImageId = note.bgImageId, contentColor = fontColor, modifier = Modifier.matchParentSize())
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Color indicator strip (if custom color)
        if (noteColor != null && noteColor != MaterialTheme.colorScheme.surfaceVariant) {
            Box(
                    modifier =
                            Modifier.width(3.dp)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(fontColor.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    note.title.ifEmpty { "New Note" },
                    color = fontColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        formatSmartDate(note.date),
                        color = fontColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                )
                if (note.content.isNotEmpty()) {
                    Text("  ", color = fontColor.copy(alpha = 0.3f), fontSize = 13.sp)
                    Text(
                            formatMarkdownToAnnotatedString(note.content.lines().first().trim(), fontColor.copy(alpha = 0.4f)),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
        if (note.isPinned) {
            Spacer(Modifier.width(8.dp))
            Icon(
                    Icons.Outlined.PushPin,
                    null,
                    tint = fontColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(13.dp)
            )
        }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteGridGroup(
        notes: List<Note>,
        onNoteClick: (Int) -> Unit,
        onDelete: (Note) -> Unit,
        onLongPress: (Note) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notes.chunked(2).forEach { rowNotes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowNotes.forEach { note ->
                    Box(modifier = Modifier.weight(1f)) {
                        NoteGridItem(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                onLongPress = { onLongPress(note) }
                        )
                    }
                }
                if (rowNotes.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridItem(note: Note, onClick: () -> Unit, onLongPress: () -> Unit) {
    val fontColor = note.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val cardColor = note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant

    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(150.dp)
                            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
            shape = RoundedCornerShape(14.dp),
            color = cardColor,
            shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (note.bgImageId != null && note.bgImageId != NoteBackgrounds.NONE) {
                NoteBackgroundRenderer(bgImageId = note.bgImageId, contentColor = fontColor, modifier = Modifier.matchParentSize())
            }
            Column(modifier = Modifier.padding(14.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                ) {
                    Text(
                            note.title.ifEmpty { "New Note" },
                            color = fontColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                                Icons.Outlined.PushPin,
                                null,
                                tint = fontColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                        )
                    }
                }
                if (note.content.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                            formatMarkdownToAnnotatedString(note.content, fontColor.copy(alpha = 0.55f)),
                            fontSize = 12.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                    )
                }
            }
            Text(
                    formatSmartDate(note.date),
                    color = fontColor.copy(alpha = 0.35f),
                    fontSize = 11.sp
            )
        }
        }
    }
}

fun formatMarkdownToAnnotatedString(text: String, fontColor: Color): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { match ->
            addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.first, match.range.first + 2)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.last - 1, match.range.last + 1)
        }
        
        val italicRegex = Regex("_(.*?)_")
        italicRegex.findAll(text).forEach { match ->
            addStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), match.range.first, match.range.last + 1)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.first, match.range.first + 1)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.last, match.range.last + 1)
        }
        
        val strikeRegex = Regex("~~(.*?)~~")
        strikeRegex.findAll(text).forEach { match ->
            addStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), match.range.first, match.range.last + 1)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.first, match.range.first + 2)
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.4f)), match.range.last - 1, match.range.last + 1)
        }
        
        val listRegex = Regex("^(•|\\[ \\]|\\[x\\])", RegexOption.MULTILINE)
        listRegex.findAll(text).forEach { match ->
            addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }
        
        addStyle(androidx.compose.ui.text.SpanStyle(color = fontColor), 0, text.length)
    }
}
