package com.example.awancoalledger.ui.screens

import androidx.compose.material3.MaterialTheme
import com.example.awancoalledger.ui.components.IOSDatePickerSheet
import com.example.awancoalledger.ui.components.IOSTimePickerSheet

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import com.example.awancoalledger.data.*
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.features.RemindersViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onBack: () -> Unit
) {
    val activeReminders by viewModel.activeReminders.collectAsState()
    val completedReminders by viewModel.completedReminders.collectAsState()
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showEditor by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    
    // Permission Handling
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "Options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditor = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New Reminder") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 2x2 Dashboard Grid
            val todayCount = activeReminders.count { isToday(it.dueDate) }
            val scheduledCount = activeReminders.count { it.dueDate != null }
            val allCount = activeReminders.size
            val flaggedCount = activeReminders.count { it.priority == ReminderPriority.HIGH }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReminderSmartCard(
                    title = "Today",
                    count = todayCount,
                    icon = Icons.Outlined.Today,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        /* Filter */ 
                    }
                )
                ReminderSmartCard(
                    title = "Scheduled",
                    count = scheduledCount,
                    icon = Icons.Outlined.CalendarMonth,
                    iconColor = ErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        /* Filter */ 
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReminderSmartCard(
                    title = "All",
                    count = allCount,
                    icon = Icons.Outlined.Inbox,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        /* Filter */ 
                    }
                )
                ReminderSmartCard(
                    title = "Flagged",
                    count = flaggedCount,
                    icon = Icons.Outlined.Flag,
                    iconColor = iOSOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        /* Filter */ 
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Active Reminders List
            Text("MY REMINDERS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    activeReminders.forEachIndexed { index, reminder ->
                        ReminderListItem(
                            reminder = reminder,
                            onToggle = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.toggleReminderCompletion(it) 
                            },
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                selectedReminder = reminder
                                showEditor = true 
                            }
                        )
                        if (index < activeReminders.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                    if (activeReminders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No reminders", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorSheet(
            reminder = selectedReminder,
            onDismiss = { 
                showEditor = false
                selectedReminder = null
            },
            onSave = { title, desc, date, priority, category, recurrence ->
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                val currentReminder = selectedReminder
                if (currentReminder == null) {
                    viewModel.addReminder(title, desc, date, priority, category)
                } else {
                    viewModel.updateReminder(currentReminder.copy(
                        title = title, note = desc, dueDate = date, remindTime = date,
                        priority = priority, category = category, recurrence = recurrence
                    ))
                }
                
                // Time-left snackbar
                if (date != null) {
                    val diff = date - System.currentTimeMillis()
                    if (diff > 0) {
                        val hours = diff / (1000 * 60 * 60)
                        val minutes = (diff / (1000 * 60)) % 60
                        val timeLeftStr = when {
                            hours > 24 -> "${hours / 24} days"
                            hours > 0 -> "$hours hours, $minutes mins"
                            else -> "$minutes minutes"
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar("Reminder set for $timeLeftStr from now")
                        }
                    }
                }
                
                showEditor = false
                selectedReminder = null
            }
        )
    }
}

@Composable
fun ReminderSmartCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                        .padding(6.dp)
                )
                Text(
                    count.toString(), 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                title, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 15.sp, 
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ReminderListItem(
    reminder: Reminder,
    onToggle: (Reminder) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, if (reminder.isCompleted) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                .clickable { onToggle(reminder) }
                .background(if (reminder.isCompleted) SuccessGreen else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (reminder.isCompleted) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                reminder.title, 
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal, 
                color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (reminder.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            reminder.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note, 
                    fontSize = 14.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            reminder.dueDate?.let { dueDate ->
                val isOverdue = dueDate < System.currentTimeMillis() && !reminder.isCompleted
                val dateStr = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(dueDate))
                Text(
                    dateStr, 
                    fontSize = 13.sp, 
                    color = if (isOverdue) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (reminder.priority != ReminderPriority.NONE) {
            Text(
                when(reminder.priority) {
                    ReminderPriority.HIGH -> "!!!"
                    ReminderPriority.MEDIUM -> "!!"
                    ReminderPriority.LOW -> "!"
                    else -> ""
                },
                color = when(reminder.priority) {
                    ReminderPriority.HIGH -> ErrorRed
                    ReminderPriority.MEDIUM -> iOSOrange
                    ReminderPriority.LOW -> SuccessGreen
                    else -> Color.Transparent
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorSheet(
    reminder: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (String, String?, Long?, ReminderPriority, ReminderCategory, ReminderRecurrence) -> Unit
) {
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var note by remember { mutableStateOf(reminder?.note ?: "") }
    var dueDate by remember { mutableStateOf<Long?>(reminder?.dueDate) }
    var priority by remember { mutableStateOf(reminder?.priority ?: ReminderPriority.NONE) }
    var category by remember { mutableStateOf(reminder?.category ?: ReminderCategory.GENERAL) }
    var recurrence by remember { mutableStateOf(reminder?.recurrence ?: ReminderRecurrence.NONE) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPrioritySheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showRecurrenceSheet by remember { mutableStateOf(false) }

    if (showDatePicker) {
        IOSDatePickerSheet(
            initialDate = dueDate ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                val cal = Calendar.getInstance()
                dueDate?.let { cal.timeInMillis = it }
                val dateCal = Calendar.getInstance().apply { timeInMillis = newDate }
                cal.set(dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH), dateCal.get(Calendar.DAY_OF_MONTH))
                dueDate = cal.timeInMillis
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        IOSTimePickerSheet(
            initialTime = dueDate ?: System.currentTimeMillis(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newTime ->
                val cal = Calendar.getInstance()
                dueDate?.let { cal.timeInMillis = it } ?: run { cal.timeInMillis = System.currentTimeMillis() }
                val timeCal = Calendar.getInstance().apply { timeInMillis = newTime }
                cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                dueDate = cal.timeInMillis
                showTimePicker = false
            }
        )
    }

    if (showPrioritySheet) {
        ModalBottomSheet(onDismissRequest = { showPrioritySheet = false }, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Select Priority", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    "Higher priority = harder to miss",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                ReminderPriority.entries.forEach { p ->
                    val priorityLabel = when(p) {
                        ReminderPriority.HIGH -> "High"
                        ReminderPriority.MEDIUM -> "Medium"
                        ReminderPriority.LOW -> "Low"
                        ReminderPriority.NONE -> "None"
                    }
                    val priorityDesc = when(p) {
                        ReminderPriority.HIGH -> "Full-screen alarm • Loud sound • Won't stop until you respond"
                        ReminderPriority.MEDIUM -> "Persistent notification • Re-alerts every 5 min (3x)"
                        ReminderPriority.LOW -> "Standard notification with sound"
                        ReminderPriority.NONE -> "Silent notification only"
                    }
                    val priorityEmoji = when(p) {
                        ReminderPriority.HIGH -> "🔴"
                        ReminderPriority.MEDIUM -> "🟠"
                        ReminderPriority.LOW -> "🟢"
                        ReminderPriority.NONE -> "⚪"
                    }
                    Surface(
                        onClick = { priority = p; showPrioritySheet = false },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (priority == p) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Flag, null, tint = when(p) {
                                ReminderPriority.HIGH -> ErrorRed
                                ReminderPriority.MEDIUM -> iOSOrange
                                ReminderPriority.LOW -> SuccessGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$priorityEmoji  $priorityLabel",
                                    fontSize = 16.sp,
                                    fontWeight = if (priority == p) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    priorityDesc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (priority == p) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (p != ReminderPriority.entries.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }

    // Similar sheets for Category and Recurrence can be improved if needed, but let's focus on the main sheet first.
    // [Truncated for brevity, assuming they remain functional but the main sheet layout changes]
    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
             Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Select Category", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))
                ReminderCategory.entries.forEach { c ->
                    Surface(onClick = { category = c; showCategorySheet = false }, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
                        Row(modifier = Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (category == c) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (c != ReminderCategory.entries.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }

    if (showRecurrenceSheet) {
        ModalBottomSheet(onDismissRequest = { showRecurrenceSheet = false }, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
             Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Select Recurrence", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))
                ReminderRecurrence.entries.forEach { r ->
                    Surface(onClick = { recurrence = r; showRecurrenceSheet = false }, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
                        Row(modifier = Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Repeat, null, tint = if (recurrence == r) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(r.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (recurrence == r) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (r != ReminderRecurrence.entries.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp) }
                Text(if (reminder == null) "New Reminder" else "Details", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextButton(onClick = { if (title.isNotBlank()) onSave(title, note, dueDate, priority, category, recurrence) }) { 
                    Text(if (reminder == null) "Add" else "Done", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp) 
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Section 1: Title & Notes
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                ) {
                    Column {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        TextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("Notes", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Details
                Text("DETAILS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                ) {
                    Column {
                        DetailRow("Date", dueDate?.let { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Not Set", Icons.Outlined.CalendarToday, MaterialTheme.colorScheme.primary) { showDatePicker = true }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DetailRow("Time", dueDate?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) } ?: "Not Set", Icons.Outlined.AccessTime, MaterialTheme.colorScheme.primary) { showTimePicker = true }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: Priority & Category
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                ) {
                    Column {
                        DetailRow("Priority", when(priority) {
                            ReminderPriority.HIGH -> "🔴 High"
                            ReminderPriority.MEDIUM -> "🟠 Medium"
                            ReminderPriority.LOW -> "🟢 Low"
                            ReminderPriority.NONE -> "None"
                        }, Icons.Outlined.Flag, when(priority) {
                            ReminderPriority.HIGH -> ErrorRed
                            ReminderPriority.MEDIUM -> iOSOrange
                            ReminderPriority.LOW -> SuccessGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }) { showPrioritySheet = true }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DetailRow("Category", category.name, Icons.Outlined.Category, MaterialTheme.colorScheme.primary) { showCategorySheet = true }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DetailRow("Repeat", recurrence.name, Icons.Outlined.Repeat, MaterialTheme.colorScheme.primary) { showRecurrenceSheet = true }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor)
                .padding(5.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Outlined.KeyboardArrowRight, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), 
            modifier = Modifier.size(20.dp)
        )
    }
}

fun isToday(millis: Long?): Boolean {
    if (millis == null) return false
    val cal1 = Calendar.getInstance()
    cal1.timeInMillis = millis
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
