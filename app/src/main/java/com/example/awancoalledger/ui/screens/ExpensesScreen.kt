package com.example.awancoalledger.ui.screens
//15,32,387,421
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var previewingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Apply Category Filter
    val displayExpenses =
            if (selectedCategory == null) allExpenses
            else allExpenses.filter { it.category == selectedCategory }
    val displayTotal = displayExpenses.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // iOS Style Large Header
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        "Expenses",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                )

                // Export Button (Triggers Bottom Sheet)
                IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExportSheet = true
                        },
                        modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = "Export", tint = PrimaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        Icons.Default.Add,
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
                    Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Text(
                                "No expenses in this period",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
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

    if (previewingExpense != null) {
        ExpensePreviewSheet(
                expense = previewingExpense!!,
                onDismiss = { previewingExpense = null },
                onEdit = {
                    editingExpense = previewingExpense
                    previewingExpense = null
                }
        )
    }

    if (editingExpense != null) {
        AddExpenseDialog(
                expense = editingExpense,
                onDismiss = { editingExpense = null },
                onConfirm = { amount, category, note, date ->
                    viewModel.deleteExpense(editingExpense!!)
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
            color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
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
                    ExpenseCategory.FOOD -> Icons.Default.Fastfood to iOSOrange
                    ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar to PrimaryBlue
                    ExpenseCategory.BUSINESS -> Icons.Default.WorkOutline to iOSPurple
                    ExpenseCategory.UTILITIES -> Icons.Default.FlashOn to SuccessGreen
                    else -> Icons.Default.ReceiptLong to Color.Gray
                }
        Box(
                modifier =
                        Modifier.size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(24.dp))
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
                            Icons.Default.Close,
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
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = PrimaryBlue,
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
                                        if (selected) PrimaryBlue
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
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
                            Icons.Default.Close,
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
                Text(expense.category.name, color = PrimaryBlue, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
