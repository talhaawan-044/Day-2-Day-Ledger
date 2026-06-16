package com.example.awancoalledger.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.ui.components.DeleteConfirmationDialog
import com.example.awancoalledger.ui.components.NoteBackgrounds
import com.example.awancoalledger.ui.components.NoteBackgroundRenderer
import com.example.awancoalledger.ui.theme.PrimaryBlue
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

private const val AUTO_SAVE_DEBOUNCE_MS = 600L
private const val MAX_UNDO_HISTORY = 50
private val FONT_SIZE_STEPS = listOf(13f, 15f, 17f, 20f, 24f, 30f)

// ─────────────────────────────────────────────────────────────────────────────
// Note Editor Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
        viewModel: LedgerViewModel,
        noteId: Int?,
        initialFolderId: Int? = null,
        onBack: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsState()
    val existingNote = remember(noteId, notes) { notes.find { it.id == noteId } }

    val themeBackground = MaterialTheme.colorScheme.background
    val themeSurface = MaterialTheme.colorScheme.surfaceVariant
    val themeOnSurface = MaterialTheme.colorScheme.onSurface
    val themeOnSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    val backgroundColors =
            remember(themeBackground, themeSurface) {
                listOf(
                        themeBackground,
                        themeSurface,
                        Color(0xFF2C3E50),
                        Color(0xFF1B4F72),
                        Color(0xFF145A32),
                        Color(0xFF641E16),
                        Color(0xFF512E5F),
                        Color(0xFF7E5109),
                        Color(0xFF117864),
                        Color(0xFF1A2530),
                        Color(0xFF0D2137),
                        Color(0xFF2D0A0A)
                )
            }

    val textColors =
            remember(themeOnSurface, themeOnSurfaceVar, themeBackground) {
                listOf(
                        themeOnSurface,
                        Color(0xFFFFD60A),
                        Color(0xFF30D158),
                        Color(0xFF64D2FF),
                        Color(0xFFFF375F),
                        Color(0xFFBF5AF2),
                        Color(0xFFFF9F0A),
                        themeOnSurfaceVar,
                        themeBackground
                )
            }

    // ── Core state ────────────────────────────────────────────────────────────
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(TextFieldValue(existingNote?.content ?: "")) }
    var isPinned by remember { mutableStateOf(existingNote?.isPinned ?: false) }
    var selectedFolderId by remember { mutableStateOf(existingNote?.folderId ?: initialFolderId) }
    var bgColor by
            remember(themeBackground) {
                mutableStateOf(existingNote?.color?.let { Color(it) } ?: themeBackground)
            }
    var fontColor by
            remember(themeOnSurface) {
                mutableStateOf(existingNote?.textColor?.let { Color(it) } ?: themeOnSurface)
            }
    var fontSize by remember { mutableFloatStateOf(existingNote?.fontSize ?: 17f) }
    var bgImageId by remember { mutableIntStateOf(existingNote?.bgImageId ?: 0) }

    // ── Undo / Redo ───────────────────────────────────────────────────────────
    val undoStack = remember { ArrayDeque<TextFieldValue>() }
    val redoStack = remember { ArrayDeque<TextFieldValue>() }
    val canUndo = undoStack.isNotEmpty()
    val canRedo = redoStack.isNotEmpty()

    fun onContentChange(newValue: TextFieldValue) {
        if (newValue.text != content.text) {
            // ── Auto-continue lists when Enter is pressed ──────────────────
            val justAddedNewline =
                    newValue.text.length == content.text.length + 1 &&
                            newValue.selection.start > 0 &&
                            newValue.text.getOrNull(newValue.selection.start - 1) == '\n'

            if (justAddedNewline) {
                val cur = newValue.selection.start
                val prevText = newValue.text.substring(0, cur - 1)
                val prevStart = prevText.lastIndexOf('\n') + 1
                val prevLine = prevText.substring(prevStart)

                val continuation: String? =
                        when {
                            prevLine.matches(Regex("^(\\d+)\\. .+")) -> {
                                val num = prevLine.substringBefore(".").toIntOrNull() ?: 1
                                "${num + 1}. "
                            }
                            prevLine.startsWith("• ") && prevLine.length > 2 -> "• "
                            (prevLine.startsWith("[ ] ") || prevLine.startsWith("[x] ")) &&
                                    prevLine.length > 4 -> "[ ] "
                            prevLine.startsWith("> ") && prevLine.length > 2 -> "> "
                            else -> null
                        }

                if (continuation != null) {
                    val inserted =
                            newValue.text.substring(0, cur) +
                                    continuation +
                                    newValue.text.substring(cur)
                    val finalValue = TextFieldValue(inserted, TextRange(cur + continuation.length))
                    undoStack.addLast(content)
                    if (undoStack.size > MAX_UNDO_HISTORY) undoStack.removeFirst()
                    redoStack.clear()
                    content = finalValue
                    return
                }
            }

            undoStack.addLast(content)
            if (undoStack.size > MAX_UNDO_HISTORY) undoStack.removeFirst()
            redoStack.clear()
        }
        content = newValue
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(content)
            content = undoStack.removeLast()
        }
    }
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(content)
            content = redoStack.removeLast()
        }
    }

    // ── Sheet / menu visibility ───────────────────────────────────────────────
    var showColorSheet by remember { mutableStateOf(false) }
    var showFontColorSheet by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }

    // ── Find state ────────────────────────────────────────────────────────────
    var findQuery by remember { mutableStateOf("") }
    var findResults by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    var findIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(findQuery, content.text) {
        if (findQuery.isNotEmpty()) {
            findResults =
                    Regex(Regex.escape(findQuery), RegexOption.IGNORE_CASE)
                            .findAll(content.text)
                            .map { it.range }
                            .toList()
            if (findIndex >= findResults.size) findIndex = 0
        } else {
            findResults = emptyList()
            findIndex = 0
        }
    }

    // ── Active format detection (line-level) ──────────────────────────────────
    val currentLine by
            remember(content.selection.start, content.text) {
                derivedStateOf {
                    val text = content.text
                    val pos = content.selection.start.coerceAtMost(text.length)
                    val start = text.lastIndexOf('\n', pos - 1) + 1
                    val end = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
                    text.substring(start, end)
                }
            }
    val isBulletActive = currentLine.startsWith("• ")
    val isChecklistActive = currentLine.startsWith("[ ] ") || currentLine.startsWith("[x] ")
    val isQuoteActive = currentLine.startsWith("> ")
    val isNumberedActive = currentLine.matches(Regex("^\\d+\\. .*"))
    val headerLevel =
            when {
                currentLine.startsWith("### ") -> 3
                currentLine.startsWith("## ") -> 2
                currentLine.startsWith("# ") -> 1
                else -> 0
            }

    // ── Stats ─────────────────────────────────────────────────────────────────
    val wordCount =
            remember(content.text) {
                if (content.text.isBlank()) 0 else content.text.trim().split("\\s+".toRegex()).size
            }
    val charCount = content.text.length
    val readingTimeMin = if (wordCount < 200) "<1" else (wordCount / 200).toString()
    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    val lastEdited = remember(existingNote) { existingNote?.date ?: System.currentTimeMillis() }

    // ── Auto-save ─────────────────────────────────────────────────────────────
    LaunchedEffect(title, content.text, isPinned, bgColor, fontColor, fontSize, bgImageId, selectedFolderId) {
        delay(AUTO_SAVE_DEBOUNCE_MS)
        if (existingNote != null && (title.isNotEmpty() || content.text.isNotEmpty())) {
            viewModel.updateNote(
                    existingNote.copy(
                            title = title,
                            content = content.text,
                            isPinned = isPinned,
                            folderId = selectedFolderId,
                            color = bgColor.toArgb(),
                            textColor = fontColor.toArgb(),
                            fontSize = fontSize,
                            bgImageId = bgImageId,
                            date = System.currentTimeMillis()
                    )
            )
        }
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    /** Toggle a simple line prefix (bullet, checklist, quote) */
    fun toggleLinePrefix(prefix: String) {
        val text = content.text
        val pos = content.selection.start.coerceAtMost(text.length)
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newText, newCursor) =
                if (line.startsWith(prefix)) {
                    val stripped =
                            text.substring(0, lineStart) +
                                    line.removePrefix(prefix) +
                                    text.substring(lineEnd)
                    Pair(stripped, (pos - prefix.length).coerceAtLeast(lineStart))
                } else {
                    val added = text.substring(0, lineStart) + prefix + text.substring(lineStart)
                    Pair(added, pos + prefix.length)
                }
        onContentChange(TextFieldValue(newText, TextRange(newCursor.coerceAtLeast(0))))
    }

    /** Toggle numbered list on the current line */
    fun toggleNumberedList() {
        val text = content.text
        val pos = content.selection.start.coerceAtMost(text.length)
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        if (line.matches(Regex("^\\d+\\. .*"))) {
            val cleaned = line.replace(Regex("^\\d+\\. "), "")
            val newText = text.substring(0, lineStart) + cleaned + text.substring(lineEnd)
            val newCursor = (pos - (line.length - cleaned.length)).coerceAtLeast(lineStart)
            onContentChange(TextFieldValue(newText, TextRange(newCursor)))
        } else {
            val prevLines = text.substring(0, lineStart).split('\n')
            val lastNum =
                    prevLines
                            .lastOrNull { it.matches(Regex("^\\d+\\. .*")) }
                            ?.substringBefore(".")
                            ?.toIntOrNull()
                            ?: 0
            val prefix = "${lastNum + 1}. "
            val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
            onContentChange(TextFieldValue(newText, TextRange(pos + prefix.length)))
        }
    }

    /** Cycle the current line through normal → H1 → H2 → H3 → normal */
    fun cycleHeader() {
        val text = content.text
        val pos = content.selection.start.coerceAtMost(text.length)
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val cleanLine = line.removePrefix("### ").removePrefix("## ").removePrefix("# ")
        val newLine =
                when {
                    line.startsWith("### ") -> cleanLine
                    line.startsWith("## ") -> "### $cleanLine"
                    line.startsWith("# ") -> "## $cleanLine"
                    else -> "# $cleanLine"
                }
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val delta = newLine.length - line.length
        onContentChange(
                TextFieldValue(
                        newText,
                        TextRange((pos + delta).coerceIn(lineStart, lineStart + newLine.length))
                )
        )
    }

    /** Wrap selection (or cursor position) with prefix + suffix */
    fun wrapSelection(prefix: String, suffix: String = prefix) {
        val text = content.text
        val sel = content.selection
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
        val newCursor =
                if (selected.isEmpty()) start + prefix.length
                else end + prefix.length + suffix.length
        onContentChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun saveAndGoBack() {
        if (existingNote == null && (title.isNotEmpty() || content.text.isNotEmpty())) {
            viewModel.addNote(
                    title = title,
                    content = content.text,
                    color = bgColor.toArgb(),
                    textColor = fontColor.toArgb(),
                    fontSize = fontSize,
                    bgImageId = bgImageId,
                    isPinned = isPinned,
                    folderId = selectedFolderId
            )
        } else if (existingNote != null && (title.isNotEmpty() || content.text.isNotEmpty())) {
            viewModel.updateNote(
                    existingNote.copy(
                            title = title,
                            content = content.text,
                            isPinned = isPinned,
                            folderId = selectedFolderId,
                            color = bgColor.toArgb(),
                            textColor = fontColor.toArgb(),
                            fontSize = fontSize,
                            bgImageId = bgImageId,
                            date = System.currentTimeMillis()
                    )
            )
        }
        onBack()
    }

    fun shareNote() {
        val text = buildString {
            if (title.isNotEmpty()) {
                append(title)
                append("\n\n")
            }
            append(content.text)
        }
        context.startActivity(
                Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title.ifEmpty { "Note" })
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        "Share Note"
                )
        )
    }

    fun duplicateNote() {
        viewModel.addNote(
                title = title.ifEmpty { "New Note" } + " (Copy)",
                content = content.text,
                color = bgColor.toArgb(),
                textColor = fontColor.toArgb(),
                fontSize = fontSize,
                isPinned = false,
                folderId = selectedFolderId
        )
    }

    fun cycleFontSize() {
        val idx = FONT_SIZE_STEPS.indexOfFirst { it >= fontSize }.takeIf { it >= 0 } ?: 0
        fontSize = FONT_SIZE_STEPS[(idx + 1) % FONT_SIZE_STEPS.size]
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scaffold UI
    // ─────────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        if (bgImageId != 0 && bgImageId != NoteBackgrounds.NONE) {
            NoteBackgroundRenderer(
                bgImageId = bgImageId,
                contentColor = fontColor,
                modifier = Modifier.matchParentSize()
            )
        }
        Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                Column {
                    TopAppBar(
                            modifier = Modifier.statusBarsPadding(),
                            title = {},
                            navigationIcon = {
                                IconButton(onClick = { saveAndGoBack() }) {
                                    Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            null,
                                            tint = PrimaryBlue
                                    )
                                }
                            },
                            actions = {
                                // Undo
                                IconButton(onClick = { undo() }, enabled = canUndo) {
                                    Icon(
                                            Icons.AutoMirrored.Filled.Undo,
                                            null,
                                            tint =
                                                    if (canUndo) PrimaryBlue
                                                    else
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant.copy(
                                                                    alpha = 0.3f
                                                            )
                                    )
                                }
                                // Redo
                                IconButton(onClick = { redo() }, enabled = canRedo) {
                                    Icon(
                                            Icons.AutoMirrored.Filled.Redo,
                                            null,
                                            tint =
                                                    if (canRedo) PrimaryBlue
                                                    else
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant.copy(
                                                                    alpha = 0.3f
                                                            )
                                    )
                                }
                                // Find toggle
                                IconButton(
                                        onClick = {
                                            showFindBar = !showFindBar
                                            if (!showFindBar) findQuery = ""
                                        }
                                ) {
                                    Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint =
                                                    if (showFindBar) PrimaryBlue
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Pin
                                IconButton(onClick = { isPinned = !isPinned }) {
                                    Icon(
                                            if (isPinned) Icons.Filled.PushPin
                                            else Icons.Outlined.PushPin,
                                            null,
                                            tint =
                                                    if (isPinned) PrimaryBlue
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // More menu
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }) {
                                        Icon(Icons.Default.MoreVert, null, tint = PrimaryBlue)
                                    }
                                    DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false },
                                            modifier =
                                                    Modifier.background(
                                                            MaterialTheme.colorScheme.surface
                                                    )
                                    ) {
                                        DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                            Icons.Default.Share,
                                                            null,
                                                            tint =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface,
                                                            modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                            "Share Note",
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    shareNote()
                                                }
                                        )
                                        DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                            Icons.Default.ContentCopy,
                                                            null,
                                                            tint =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface,
                                                            modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                            "Duplicate Note",
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    duplicateNote()
                                                }
                                        )
                                        DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                            Icons.Default.FolderOpen,
                                                            null,
                                                            tint =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface,
                                                            modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                            "Move to Folder",
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurface
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    showFolderSheet = true
                                                }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                        DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                            Icons.Default.Delete,
                                                            null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                            "Delete Note",
                                                            color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    showDeleteConfirm = true
                                                }
                                        )
                                    }
                                }
                                // Done
                                TextButton(onClick = { saveAndGoBack() }) {
                                    Text(
                                            "Done",
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 17.sp
                                    )
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color.Transparent
                                    )
                    )

                    // ── Animated Find Bar ────────────────────────────────────────
                    AnimatedVisibility(
                            visible = showFindBar,
                            enter =
                                    expandVertically(
                                            spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeIn(),
                            exit =
                                    shrinkVertically(
                                            spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeOut()
                    ) {
                        FindBar(
                                query = findQuery,
                                onQueryChange = { findQuery = it },
                                matchCount = findResults.size,
                                currentMatch = if (findResults.isNotEmpty()) findIndex + 1 else 0,
                                onPrevious = {
                                    if (findResults.isNotEmpty())
                                            findIndex =
                                                    (findIndex - 1 + findResults.size) %
                                                            findResults.size
                                },
                                onNext = {
                                    if (findResults.isNotEmpty())
                                            findIndex = (findIndex + 1) % findResults.size
                                },
                                onClose = {
                                    showFindBar = false
                                    findQuery = ""
                                }
                        )
                    }
                }
            },
            bottomBar = {
                Column(
                        modifier =
                                Modifier.background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.95f
                                                )
                                        )
                                        .imePadding()
                                        .navigationBarsPadding()
                ) {
                    // ── Stats row ────────────────────────────────────────────────
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorStat("$wordCount ${if (wordCount == 1) "word" else "words"}")
                        EditorStatDot()
                        EditorStat("$charCount chars")
                        EditorStatDot()
                        EditorStat("~$readingTimeMin min read")
                    }

                    HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                    )

                    // ── Formatting toolbar ───────────────────────────────────────
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Group 1 – Blocks
                        FormatButton(
                                Icons.Default.Checklist,
                                fontColor,
                                isChecklistActive,
                                "Checklist"
                        ) { toggleLinePrefix("[ ] ") }
                        FormatButton(
                                Icons.Default.FormatListBulleted,
                                fontColor,
                                isBulletActive,
                                "Bullet"
                        ) { toggleLinePrefix("• ") }
                        FormatButton(
                                Icons.Default.FormatListNumbered,
                                fontColor,
                                isNumberedActive,
                                "Numbered"
                        ) { toggleNumberedList() }
                        FormatButton(Icons.Default.FormatQuote, fontColor, isQuoteActive, "Quote") {
                            toggleLinePrefix("> ")
                        }

                        EditorToolbarDivider()

                        // Group 2 – Inline
                        FormatButton(Icons.Default.FormatBold, fontColor, false, "Bold") {
                            wrapSelection("**")
                        }
                        FormatButton(Icons.Default.FormatItalic, fontColor, false, "Italic") {
                            wrapSelection("_")
                        }
                        FormatButton(Icons.Default.StrikethroughS, fontColor, false, "Strike") {
                            wrapSelection("~~")
                        }
                        FormatButton(Icons.Default.Code, fontColor, false, "Inline Code") {
                            wrapSelection("`")
                        }

                        EditorToolbarDivider()

                        // Group 3 – Headers
                        HeaderCycleButton(level = headerLevel, fontColor = fontColor) {
                            cycleHeader()
                        }

                        EditorToolbarDivider()

                        // Group 4 – Style
                        // Font size
                        Box(
                                modifier =
                                        Modifier.clip(RoundedCornerShape(8.dp))
                                                .clickable { cycleFontSize() }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    "${fontSize.toInt()}",
                                    color = fontColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Background color swatch
                        Box(
                                modifier =
                                        Modifier.size(40.dp).clip(CircleShape).clickable {
                                            showColorSheet = true
                                        },
                                contentAlignment = Alignment.Center
                        ) {
                            Box(
                                    modifier =
                                            Modifier.size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                            if (bgColor != themeBackground &&
                                                                            bgColor != themeSurface
                                                            )
                                                                    bgColor
                                                            else
                                                                    MaterialTheme.colorScheme
                                                                            .onSurfaceVariant.copy(
                                                                            alpha = 0.2f
                                                                    )
                                                    )
                                                    .border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                    alpha = 0.35f
                                                            ),
                                                            CircleShape
                                                    )
                            )
                        }

                        // Font color
                        Box(
                                modifier =
                                        Modifier.size(40.dp).clip(CircleShape).clickable {
                                            showFontColorSheet = true
                                        },
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    Icons.Default.FormatColorText,
                                    null,
                                    tint = fontColor,
                                    modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 22.dp)
                                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(10.dp))

            // ── Date ──────────────────────────────────────────────────────────
            Text(
                    sdf.format(Date(lastEdited)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // ── Title ─────────────────────────────────────────────────────────
            BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle =
                            TextStyle(
                                    color = fontColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 34.sp
                            ),
                    cursorBrush = SolidColor(PrimaryBlue),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                    "Title",
                                    color = fontColor.copy(alpha = 0.25f),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                        inner()
                    }
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = fontColor.copy(alpha = 0.07f), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            // ── Content ───────────────────────────────────────────────────────
            BasicTextField(
                    value = content,
                    onValueChange = { onContentChange(it) },
                    textStyle =
                            TextStyle(
                                    color = fontColor,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.65f).sp
                            ),
                    cursorBrush = SolidColor(PrimaryBlue),
                    visualTransformation =
                            NoteMarkdownTransformation(
                                    fontColor = fontColor,
                                    findQuery = findQuery,
                                    findRanges = findResults,
                                    currentMatch = findIndex
                            ),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 300.dp),
                    decorationBox = { inner ->
                        if (content.text.isEmpty()) {
                            Text(
                                    "Start writing…",
                                    color = fontColor.copy(alpha = 0.2f),
                                    fontSize = fontSize.sp
                            )
                        }
                        inner()
                    }
            )

            Spacer(Modifier.height(120.dp))
        }
    }

    // ── Bottom Sheets & Dialogs ───────────────────────────────────────────────

    if (showColorSheet) {
        EnhancedBackgroundSheet(
                colors = backgroundColors,
                selectedColor = bgColor,
                selectedBgImageId = bgImageId,
                onDismiss = { showColorSheet = false },
                onSelectColor = {
                    bgColor = it
                    bgImageId = NoteBackgrounds.NONE
                    showColorSheet = false
                },
                onSelectBgImage = {
                    bgImageId = it
                    if (it != NoteBackgrounds.NONE && bgColor == themeBackground) {
                        // Optional: Keep background color transparent/light when pattern is chosen, but handled by renderer.
                    }
                    showColorSheet = false
                }
        )
    }

    if (showFontColorSheet) {
        EnhancedColorSheet(
                title = "Text Color",
                colors = textColors,
                selectedColor = fontColor,
                onDismiss = { showFontColorSheet = false },
                onSelect = {
                    fontColor = it
                    showFontColorSheet = false
                }
        )
    }

    if (showFolderSheet) {
        val folders by viewModel.allFolders.collectAsState()
        FolderPickerSheet(
                folders = folders,
                selectedFolderId = selectedFolderId,
                onDismiss = { showFolderSheet = false },
                onSelect = { folderId ->
                    selectedFolderId = folderId
                    showFolderSheet = false
                }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
                title = "Delete Note",
                message = "Are you sure you want to delete this note? This cannot be undone.",
                onConfirm = {
                    existingNote?.let { viewModel.deleteNote(it) }
                    showDeleteConfirm = false
                    onBack()
                },
                onDismiss = { showDeleteConfirm = false }
        )
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Find Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FindBar(
        query: String,
        onQueryChange: (String) -> Unit,
        matchCount: Int,
        currentMatch: Int,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onClose: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    Icons.Default.Search,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle =
                            TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                            ),
                    cursorBrush = SolidColor(PrimaryBlue),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                    "Find in note…",
                                    color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.45f
                                            ),
                                    fontSize = 15.sp
                            )
                        }
                        inner()
                    }
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                        if (matchCount > 0) "$currentMatch / $matchCount" else "No results",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontSize = 12.sp
                )
                IconButton(
                        onClick = onPrevious,
                        enabled = matchCount > 0,
                        modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                            Icons.Default.KeyboardArrowUp,
                            null,
                            tint =
                                    if (matchCount > 0) PrimaryBlue
                                    else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.3f
                                            ),
                            modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                        onClick = onNext,
                        enabled = matchCount > 0,
                        modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            tint =
                                    if (matchCount > 0) PrimaryBlue
                                    else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.3f
                                            ),
                            modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormatButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        fontColor: Color,
        isActive: Boolean,
        tooltip: String,
        onClick: () -> Unit
) {
    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                    if (isActive) PrimaryBlue.copy(alpha = 0.14f)
                                    else Color.Transparent
                            )
                            .clickable(onClick = onClick)
                            .size(40.dp),
            contentAlignment = Alignment.Center
    ) {
        Icon(
                icon,
                contentDescription = tooltip,
                tint = if (isActive) PrimaryBlue else fontColor,
                modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun HeaderCycleButton(level: Int, fontColor: Color, onClick: () -> Unit) {
    val label =
            when (level) {
                1 -> "H1"
                2 -> "H2"
                3 -> "H3"
                else -> "H"
            }
    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                    if (level > 0) PrimaryBlue.copy(alpha = 0.14f)
                                    else Color.Transparent
                            )
                            .clickable(onClick = onClick)
                            .padding(horizontal = 11.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                label,
                color = if (level > 0) PrimaryBlue else fontColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditorToolbarDivider() {
    VerticalDivider(
            modifier = Modifier.height(22.dp).padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
}

@Composable
private fun EditorStat(text: String) {
    Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
    )
}

@Composable
private fun EditorStatDot() {
    Text(
            "  ·  ",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            fontSize = 11.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Markdown Visual Transformation  (enhanced)
// ─────────────────────────────────────────────────────────────────────────────
//
// Supported syntax (all use OffsetMapping.Identity — no character removal):
//   Headers      : # H1    ## H2    ### H3
//   Blockquote   : > text
//   Inline code  : `code`
//   Highlight    : ==text==
//   Bold         : **text**
//   Italic       : _text_
//   Strikethrough: ~~text~~
//   Bullet       : • item
//   Numbered     : 1. item
//   Checklist    : [ ] item  /  [x] item
//   HR           : ---
//   Find matches : highlighted in yellow / orange
// ─────────────────────────────────────────────────────────────────────────────

class NoteMarkdownTransformation(
        private val fontColor: Color,
        private val findQuery: String = "",
        private val findRanges: List<IntRange> = emptyList(),
        private val currentMatch: Int = 0
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val result = buildAnnotatedString {
            append(text.text)

            // H1 – lines starting with exactly "# " (not "## ")
            Regex("^# (?!#).+$", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp),
                        m.range.first,
                        m.range.last + 1
                )
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.28f), fontSize = 23.sp),
                        m.range.first,
                        (m.range.first + 2).coerceAtMost(m.range.last + 1)
                )
            }

            // H2 – "## " (not "### ")
            Regex("^## (?!#).+$", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        m.range.first,
                        m.range.last + 1
                )
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.28f), fontSize = 20.sp),
                        m.range.first,
                        (m.range.first + 3).coerceAtMost(m.range.last + 1)
                )
            }

            // H3 – "### "
            Regex("^### .+$", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                        m.range.first,
                        m.range.last + 1
                )
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.28f), fontSize = 17.sp),
                        m.range.first,
                        (m.range.first + 4).coerceAtMost(m.range.last + 1)
                )
            }

            // Blockquote – "> "
            Regex("^> .+$", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(
                                color = PrimaryBlue.copy(alpha = 0.82f),
                                fontStyle = FontStyle.Italic
                        ),
                        m.range.first,
                        m.range.last + 1
                )
                addStyle(
                        SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold),
                        m.range.first,
                        (m.range.first + 2).coerceAtMost(m.range.last + 1)
                )
            }

            // Inline code – `...`
            Regex("`([^`\n]+)`").findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = fontColor.copy(alpha = 0.07f),
                                color = fontColor.copy(alpha = 0.88f)
                        ),
                        m.range.first,
                        m.range.last + 1
                )
                if (m.range.last > m.range.first) {
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.3f)),
                            m.range.first,
                            m.range.first + 1
                    )
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.3f)),
                            m.range.last,
                            m.range.last + 1
                    )
                }
            }

            // Highlight – ==...==
            Regex("==((?!=).+?)==").findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(background = Color(0xFFFFD60A).copy(alpha = 0.26f)),
                        m.range.first,
                        m.range.last + 1
                )
                if (m.range.last - m.range.first >= 4) {
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.first,
                            m.range.first + 2
                    )
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.last - 1,
                            m.range.last + 1
                    )
                }
            }

            // Bold – **...**
            Regex("\\*\\*((?!\\*).+?)\\*\\*").findAll(text.text).forEach { m ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
                if (m.range.last - m.range.first >= 4) {
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.first,
                            m.range.first + 2
                    )
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.last - 1,
                            m.range.last + 1
                    )
                }
            }

            // Italic – _..._
            Regex("_((?!_).+?)_").findAll(text.text).forEach { m ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                        m.range.first,
                        m.range.first + 1
                )
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                        m.range.last,
                        m.range.last + 1
                )
            }

            // Strikethrough – ~~...~~
            Regex("~~((?!~).+?)~~").findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                color = fontColor.copy(alpha = 0.55f)
                        ),
                        m.range.first,
                        m.range.last + 1
                )
                if (m.range.last - m.range.first >= 4) {
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.first,
                            m.range.first + 2
                    )
                    addStyle(
                            SpanStyle(color = fontColor.copy(alpha = 0.28f)),
                            m.range.last - 1,
                            m.range.last + 1
                    )
                }
            }

            // Bullet – "• "
            Regex("^• ", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.ExtraBold),
                        m.range.first,
                        m.range.last + 1
                )
            }

            // Numbered list – "1. "
            Regex("^\\d+\\. ", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold),
                        m.range.first,
                        m.range.last + 1
                )
            }

            // Unchecked – "[ ] "
            Regex("^\\[ \\] ", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.42f)),
                        m.range.first,
                        m.range.last + 1
                )
            }

            // Checked – "[x] "
            Regex("^\\[x\\] ", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold),
                        m.range.first,
                        m.range.last + 1
                )
            }

            // Horizontal rule – "---"
            Regex("^---$", RegexOption.MULTILINE).findAll(text.text).forEach { m ->
                addStyle(
                        SpanStyle(color = fontColor.copy(alpha = 0.18f)),
                        m.range.first,
                        m.range.last + 1
                )
            }

            // ── Find highlights ───────────────────────────────────────────────
            if (findQuery.isNotEmpty()) {
                findRanges.forEachIndexed { index, range ->
                    val bg =
                            if (index == currentMatch) Color(0xFFFF9F0A).copy(alpha = 0.42f)
                            else Color(0xFFFFD60A).copy(alpha = 0.28f)
                    val safeEnd = (range.last + 1).coerceAtMost(text.text.length)
                    if (range.first < safeEnd)
                            addStyle(SpanStyle(background = bg), range.first, safeEnd)
                }
            }
        }
        return TransformedText(result, OffsetMapping.Identity)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Enhanced Color Selector Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedBackgroundSheet(
    colors: List<Color>,
    selectedColor: Color,
    selectedBgImageId: Int,
    onSelectColor: (Color) -> Unit,
    onSelectBgImage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 14.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 44.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Solid Colors", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Textures & Patterns", fontWeight = FontWeight.SemiBold) }
                )
            }
            Spacer(Modifier.height(22.dp))
            
            if (selectedTab == 0) {
                val columns = 5
                colors.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { color ->
                            val isSelected = color == selectedColor && selectedBgImageId == NoteBackgrounds.NONE
                            Box(
                                modifier = Modifier.size(52.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, PrimaryBlue, CircleShape)
                                        else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                                    )
                                    .clickable { onSelectColor(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = if (color.luminance() > 0.4f) Color.Black.copy(alpha = 0.75f) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.size(52.dp)) }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            } else {
                val columns = 3
                NoteBackgrounds.allBackgrounds.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { bgId ->
                            val isSelected = bgId == selectedBgImageId
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                                            else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        )
                                        .clickable { onSelectBgImage(bgId) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bgId != NoteBackgrounds.NONE) {
                                        NoteBackgroundRenderer(
                                            bgImageId = bgId,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (isSelected) {
                                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(PrimaryBlue), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    NoteBackgrounds.getName(bgId),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedColorSheet(
        title: String,
        colors: List<Color>,
        selectedColor: Color,
        onDismiss: () -> Unit,
        onSelect: (Color) -> Unit
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            dragHandle = {
                Box(
                        modifier =
                                Modifier.padding(top = 14.dp)
                                        .size(width = 38.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                )
            }
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 44.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(22.dp))

            val columns = 5
            colors.chunked(columns).forEach { row ->
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                                modifier =
                                        Modifier.size(52.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                        if (isSelected)
                                                                Modifier.border(
                                                                        3.dp,
                                                                        PrimaryBlue,
                                                                        CircleShape
                                                                )
                                                        else
                                                                Modifier.border(
                                                                        1.dp,
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface.copy(
                                                                                alpha = 0.12f
                                                                        ),
                                                                        CircleShape
                                                                )
                                                )
                                                .clickable { onSelect(color) },
                                contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint =
                                                if (color.luminance() > 0.4f)
                                                        Color.Black.copy(alpha = 0.75f)
                                                else Color.White,
                                        modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.size(52.dp)) }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

/** Approximate luminance helper so checkmarks stay readable on any swatch */
private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

// ─────────────────────────────────────────────────────────────────────────────
// Folder Picker Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerSheet(
        folders: List<com.example.awancoalledger.data.Folder>,
        selectedFolderId: Int?,
        onDismiss: () -> Unit,
        onSelect: (Int?) -> Unit
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            dragHandle = {
                Box(
                        modifier =
                                Modifier.padding(top = 14.dp)
                                        .size(width = 38.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                )
            }
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 44.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                    "Move to Folder",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(14.dp))

            FolderPickerRow(
                    name = "Uncategorized",
                    icon = Icons.Default.FolderOff,
                    isSelected = selectedFolderId == null,
                    onClick = { onSelect(null) }
            )
            HorizontalDivider(
                    modifier = Modifier.padding(start = 50.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
            )

            folders.forEach { folder ->
                FolderPickerRow(
                        name = folder.name,
                        icon = Icons.Default.Folder,
                        isSelected = selectedFolderId == folder.id,
                        onClick = { onSelect(folder.id) }
                )
                HorizontalDivider(
                        modifier = Modifier.padding(start = 50.dp),
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun FolderPickerRow(
        name: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isSelected: Boolean,
        onClick: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
                name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
        )
        if (isSelected)
                Icon(Icons.Default.Check, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
    }
}
