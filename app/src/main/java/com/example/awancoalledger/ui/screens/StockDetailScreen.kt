package com.example.awancoalledger.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Stock
import com.example.awancoalledger.data.StockEntry
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.ErrorRed
import com.example.awancoalledger.ui.theme.SuccessGreen
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
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
                            Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                containerColor = MaterialTheme.colorScheme.primary,
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

    editingEntry?.let { entry ->
        AddStockEntryDialog(
            mineName = stock?.mineName ?: "Stock",
            initialWeight = entry.weight.toString(),
            initialWarehouse = entry.warehouse,
            isEdit = true,
            onDismiss = { editingEntry = null },
            onConfirm = { weight, warehouse ->
                viewModel.deleteStockEntry(entry)
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
    val df = java.text.DecimalFormat("#,###.##")
    val progress = if (stock.peakWeight > 0) (stock.totalWeight / stock.peakWeight).toFloat() else 0f
    
    val isHealthy = progress > 0.3f
    val statusColor = if (isHealthy) SuccessGreen else ErrorRed
    val statusText = if (isHealthy) "HEALTHY" else "LOW STOCK"
    val statusIcon = if (isHealthy) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            // Top Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CURRENT STOCK", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                // Status Pill
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(statusIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    df.format(stock.totalWeight), 
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp
                )
                Spacer(Modifier.width(6.dp))
                Text("t", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Progress System
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inventory Level", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = if (isHealthy) MaterialTheme.colorScheme.primary else ErrorRed,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            
            // Sub-metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PEAK RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${df.format(stock.peakWeight)} t", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }

                HorizontalDivider(modifier = Modifier.width(1.dp).height(32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warehouse, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("LAST LOCATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stock.lastWarehouse ?: "N/A", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
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
                    containerColor = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.primary,
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
