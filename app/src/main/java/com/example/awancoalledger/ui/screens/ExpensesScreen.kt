package com.example.awancoalledger.ui.screens
//15,32,387,421
import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Expense
import com.example.awancoalledger.data.ExpenseCategory
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.utils.capitalize
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // State Collections
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList<Expense>())

    // UI States
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var previewingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Apply Category & Search Filters
    val displayExpenses = allExpenses.filter { expense ->
        val matchesCategory = selectedCategory == null || expense.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() || 
            expense.note.orEmpty().contains(searchQuery, ignoreCase = true) ||
            expense.amount.toString().contains(searchQuery)
        matchesCategory && matchesSearch
    }
    val displayTotal = displayExpenses.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            com.example.awancoalledger.ui.components.ScreenHeader(
                title = "Expenses",
                onBack = { backDispatcher?.onBackPressed() },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExportSheet = true
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Outlined.IosShare, contentDescription = "Export", tint = Color.White)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            TotalExpensesHeroCard(displayTotal, displayExpenses.size)

            Spacer(modifier = Modifier.height(24.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(2.dp, RoundedCornerShape(16.dp)),
                placeholder = { Text("Search expenses...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Categories Header & Scroll
            Text(
                    "CATEGORIES",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPill("All", selectedCategory == null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedCategory = null
                }
                ExpenseCategory.entries.forEach { category ->
                    CategoryPill(category.name.capitalize(), selectedCategory == category) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedCategory = category
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // iOS Prominent Add Button
            Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAddDialog = true
                    },
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                            )
            ) {
                Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        "Add New Expense",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background,
                        fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Transactions List
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        "RECENT TRANSACTIONS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "${displayExpenses.size} items",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                    modifier =
                            Modifier.clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (displayExpenses.isEmpty()) {
                    com.example.awancoalledger.ui.components.EmptyStateCard(
                        icon = Icons.Outlined.Payments,
                        title = "No Expenses Found",
                        description = "There are no recorded expenses in this period.",
                        actionText = "Start Tracking Your Expenses",
                        onAction = { showAddDialog = true }
                    )
                } else {
                    displayExpenses.sortedByDescending { it.date }.forEachIndexed { index, expense
                        ->
                        SwipeableItem(
                                
                                onEdit = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editingExpense = expense
                                },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    expenseToDelete = expense
                                },
                                content = {
                                    ExpenseRow(expense) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        previewingExpense = expense
                                    }
                                },
                        )
                        if (index < displayExpenses.size - 1) {
                            HorizontalDivider(
                                    modifier = Modifier.padding(start = 64.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- NATIVE EXPORT BOTTOM SHEET ---
    if (showExportSheet) {
        ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text(
                        "Export Reports",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        "Generate offline Apple-quality PDFs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        "QUICK PDF EXPORT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportOptionButton(modifier = Modifier.weight(1f), title = "1 Month") {
                        val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                        viewModel.exportExpensesToNativePdf(
                                context,
                                allExpenses.filter { it.date >= cutoff },
                                "Last 30 Days"
                        )
                        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                        showExportSheet = false
                    }
                    ExportOptionButton(modifier = Modifier.weight(1f), title = "2 Months") {
                        val cutoff = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
                        viewModel.exportExpensesToNativePdf(
                                context,
                                allExpenses.filter { it.date >= cutoff },
                                "Last 2 Months"
                        )
                        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                        showExportSheet = false
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportOptionButton(modifier = Modifier.weight(1f), title = "6 Months") {
                        val cutoff = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
                        viewModel.exportExpensesToNativePdf(
                                context,
                                allExpenses.filter { it.date >= cutoff },
                                "Last 6 Months"
                        )
                        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                        showExportSheet = false
                    }
                    ExportOptionButton(modifier = Modifier.weight(1f), title = "1 Year") {
                        val cutoff = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
                        viewModel.exportExpensesToNativePdf(
                                context,
                                allExpenses.filter { it.date >= cutoff },
                                "Last Year"
                        )
                        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
                        showExportSheet = false
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        "CUSTOM RANGE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                        onClick = {
                            viewModel.exportExpensesToNativePdf(
                                    context,
                                    displayExpenses,
                                    "Custom Filter"
                            )
                            Toast.makeText(
                                            context,
                                            "Custom PDF saved to Downloads",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            showExportSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Current View", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { amount, category, note, date ->
                    viewModel.addExpense(amount, category, note, date)
                    showAddDialog = false
                }
        )
    }

    previewingExpense?.let { expense ->
        ExpensePreviewSheet(
                expense = expense,
                onDismiss = { previewingExpense = null },
                onEdit = {
                    editingExpense = previewingExpense
                    previewingExpense = null
                }
        )
    }

    editingExpense?.let { expense ->
        AddExpenseDialog(
                expense = expense,
                onDismiss = { editingExpense = null },
                onConfirm = { amount, category, note, date ->
                    viewModel.deleteExpense(expense)
                    viewModel.addExpense(amount, category, note, date)
                    editingExpense = null
                }
        )
    }

    expenseToDelete?.let { expense ->
        DeleteConfirmationDialog(
                title = "Delete Expense",
                message =
                        "Are you sure you want to delete this Rs. ${expense.amount.toInt()} expense? This will be removed from your summaries.",
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteExpense(expense)
                    expenseToDelete = null
                },
                onDismiss = { expenseToDelete = null }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun ExportOptionButton(modifier: Modifier = Modifier, title: String, onClick: () -> Unit) {
    Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                    )
    ) { Text(title, fontWeight = FontWeight.Bold) }
}

@Composable
fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() },
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
    ) {
        Text(
                label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color =
                        if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
        )
    }
}

@Composable
fun ExpenseStatCard(
        modifier: Modifier,
        title: String,
        value: String,
        subValue: String,
        icon: ImageVector,
        iconColor: Color
) {
    Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    subValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tintColor) =
                when (expense.category) {
                    ExpenseCategory.FOOD -> Icons.Outlined.Fastfood to iOSOrange
                    ExpenseCategory.TRANSPORT -> Icons.Outlined.DirectionsCar to MaterialTheme.colorScheme.primary
                    ExpenseCategory.BUSINESS -> Icons.Outlined.WorkOutline to iOSPurple
                    ExpenseCategory.UTILITIES -> Icons.Outlined.FlashOn to SuccessGreen
                    else -> Icons.Outlined.ReceiptLong to Color.Gray
                }
        Box(
                modifier =
                        Modifier.size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tintColor),
                contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    expense.category.name.capitalize(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
            )
            Text(
                    "${expense.note ?: "No note"} • ${df.format(Date(expense.date))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
            )
        }
        Text(
                "Rs. ${expense.amount.toInt()}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
        expense: Expense? = null,
        onDismiss: () -> Unit,
        onConfirm: (Double, ExpenseCategory, String?, Long) -> Unit
) {
    var amount by remember {
        mutableStateOf(expense?.amount?.let { if (it == 0.0) "" else it.toInt().toString() } ?: "")
    }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: ExpenseCategory.FOOD) }
    var selectedDate by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        com.example.awancoalledger.ui.components.IOSDatePickerSheet(
                initialDate = selectedDate,
                onDismiss = { showDatePicker = false },
                onDateSelected = {
                    selectedDate = it
                    showDatePicker = false
                }
        )
    }

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        if (expense == null) "New Expense" else "Edit Expense",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                        onClick = onDismiss,
                        modifier =
                                Modifier.background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                        )
                                        .size(32.dp)
                ) {
                    Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Date Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        "DATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                        onClick = { showDatePicker = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sdf = remember {
                            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        }
                        Text(
                                sdf.format(Date(selectedDate)),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                        )
                        Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            PremiumInput(
                    label = "Amount",
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    prefix = {
                        Text(
                                "Rs. ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
            )

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        "CATEGORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        val selected = category == cat
                        Surface(
                                modifier =
                                        Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                            category = cat
                                        },
                                color =
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.3f
                                                ),
                                shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                    cat.name.capitalize(),
                                    modifier =
                                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    color =
                                            if (selected) Color.White
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            PremiumInput(label = "Note (Optional)", value = note, onValueChange = { note = it })

            Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) onConfirm(amt, category, note.ifEmpty { null }, selectedDate)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = amount.isNotEmpty()
            ) {
                Text(
                        if (expense == null) "Add Expense" else "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensePreviewSheet(expense: Expense, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                            "EXPENSE DETAILS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            "Rs. ${String.format(Locale.getDefault(), "%,.0f", expense.amount)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(
                        onClick = onDismiss,
                        modifier =
                                Modifier.size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                    modifier =
                            Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f
                                            )
                                    )
                                    .padding(16.dp)
            ) {
                Text(expense.category.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        expense.note ?: "No note provided",
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormatter = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                Text(
                        dateFormatter.format(Date(expense.date)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                        "Edit Expense",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TotalExpensesHeroCard(total: Double, count: Int) {
    val animatedTotal by animateFloatAsState(
        targetValue = total.toFloat(),
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessVeryLow),
        label = "totalExpenses"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL EXPENSES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Status Pill
                Surface(
                    color = ErrorRed,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Rs.",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%,.0f", animatedTotal),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ENTRIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("$count", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }

                HorizontalDivider(modifier = Modifier.width(1.dp).height(32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AVG EXPENSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    val avg = if (count > 0) total / count else 0.0
                    Text(String.format("%,.0f", avg), fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
