package com.example.awancoalledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Stock
import com.example.awancoalledger.data.StockEntry
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.PrimaryBlue
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: LedgerViewModel,
    stockId: Int,
    onBack: () -> Unit
) {
    val stocks by viewModel.allStocks.collectAsState()
    val entries by viewModel.selectedStockEntries.collectAsState()
    val stock = stocks.find { it.id == stockId }
    var entryToDelete by remember { mutableStateOf<StockEntry?>(null) }
    var editingEntry by remember { mutableStateOf<StockEntry?>(null) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(stockId) {
        viewModel.selectStock(stockId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { 
                    Text(
                        stock?.mineName ?: "Stock Detail", 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )

            if (stock == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Stock not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Header
                    item {
                        StockSummaryHeader(stock)
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ENTRY HISTORY",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (entries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No history available", color = Color.DarkGray)
                            }
                        }
                    } else {
                        items(entries, key = { it.id }) { entry ->
                            SwipeableItem(
                                onEdit = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    editingEntry = entry 
                                },
                                onDelete = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    entryToDelete = entry 
                                },
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                StockEntryCard(entry)
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }

        // Add Entry Floating Action Button
        if (stock != null) {
            FloatingActionButton(
                onClick = { showAddEntryDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding(),
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, "Add Entry")
            }
        }
    }

    if (showAddEntryDialog && stock != null) {
        AddStockEntryDialog(
            mineName = stock.mineName,
            onDismiss = { showAddEntryDialog = false },
            onConfirm = { weight, warehouse ->
                viewModel.addStockEntry(mine = stock.mineName, weight = weight, warehouse = warehouse)
                showAddEntryDialog = false
            }
        )
    }

    if (editingEntry != null) {
        AddStockEntryDialog(
            mineName = stock?.mineName ?: "Stock",
            initialWeight = editingEntry?.weight?.toString() ?: "",
            initialWarehouse = editingEntry?.warehouse ?: "",
            isEdit = true,
            onDismiss = { editingEntry = null },
            onConfirm = { weight, warehouse ->
                viewModel.deleteStockEntry(editingEntry!!)
                viewModel.addStockEntry(mine = stock?.mineName ?: "", weight = weight, warehouse = warehouse)
                editingEntry = null
            }
        )
    }

    entryToDelete?.let { entry ->
        DeleteConfirmationDialog(
            title = "Delete Entry",
            message = "Remove this stock entry of ${entry.weight} tons? This will update the available balance.",
            onConfirm = {
                viewModel.deleteStockEntry(entry)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }
}

@Composable
fun StockSummaryHeader(stock: Stock) {
    val df = DecimalFormat("#,###.##")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, Color(0xFF000000))
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT STOCK", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(df.format(stock.totalWeight), color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("t", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("PEAK RECORD", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${df.format(stock.peakWeight)} t", color = PrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Progress System
            val progress = if (stock.peakWeight > 0) (stock.totalWeight / stock.peakWeight).toFloat() else 0f
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inventory Level", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warehouse, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Last Warehouse Location: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(stock.lastWarehouse ?: "N/A", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AddStockEntryDialog(
    mineName: String,
    initialWeight: String = "",
    initialWarehouse: String = "",
    isEdit: Boolean = false,
    onDismiss: () -> Unit, 
    onConfirm: (Double, String) -> Unit
) {
    var weight by remember { mutableStateOf(initialWeight) }
    var warehouse by remember { mutableStateOf(initialWarehouse) }
    
    val isFormValid = weight.toDoubleOrNull() != null && warehouse.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { 
            Text(
                if (isEdit) "Edit Entry" else "Add to $mineName", 
                color = MaterialTheme.colorScheme.onSurface, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (isEdit) "Update the stock entry details." else "Record a new arrival of coal for this specific mine.", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 14.sp
                )
                PremiumInput(label = "Weight (Tons)", value = weight, onValueChange = { weight = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                PremiumInput(label = "Warehouse Location", value = warehouse, onValueChange = { warehouse = it })
            }

        },
        confirmButton = {
            Button(
                onClick = { if (isFormValid) onConfirm(weight.toDouble(), warehouse) },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEdit) "Save Changes" else "Record Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        }
    )
}

@Composable
fun StockEntryCard(entry: StockEntry) {
    val df = DecimalFormat("#,###.##")
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    df.format(entry.weight) + " Tons",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    entry.warehouse,
                    color = PrimaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    sdf.format(Date(entry.date)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
