package com.example.awancoalledger.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Stock
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.PrimaryBlue
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: LedgerViewModel, onNavigateToStockDetail: (Int) -> Unit) {
    val stocks by viewModel.allStocks.collectAsState()
    val totalWeight by viewModel.totalInventoryWeight.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf(false) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val filteredStocks = stocks.filter { it.mineName.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            Column(
                    modifier =
                            Modifier.background(MaterialTheme.colorScheme.background)
                                    .statusBarsPadding()
            ) {
                val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                com.example.awancoalledger.ui.components.ScreenHeader(
                    title = "Stock",
                    onBack = { backDispatcher?.onBackPressed() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumInput(
                            label = "Filter Mines",
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = {
                                Icon(
                                        Icons.Outlined.Search,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                    )

                    // View Toggle
                    IconButton(
                            onClick = {
                                haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                .LongPress
                                )
                                isGridView = !isGridView
                            },
                            modifier =
                                    Modifier.size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                                if (isGridView) Icons.AutoMirrored.Outlined.List
                                else Icons.Outlined.GridView,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Inventory Card - Premium Redesign
                Surface(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border =
                                BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                ) {
                    Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                    "TOTAL RECORD",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                        DecimalFormat("#,###.##").format(totalWeight),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                )
                                Text(
                                        " tons",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                        }

                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    Icons.Outlined.TrendingUp,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Add Stock Button - Premium Labeling
                Button(
                        onClick = {
                            haptic.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                            showAddStockDialog = true
                        },
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onBackground,
                                        contentColor = MaterialTheme.colorScheme.background
                                ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                            "Add New Mine",
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                    )
                }
            }

            // Main Content Area
            if (filteredStocks.isEmpty()) {
                com.example.awancoalledger.ui.components.EmptyStateCard(
                    icon = Icons.Outlined.Inventory2,
                    title = if (searchQuery.isNotEmpty()) "No Results" else "Inventory Empty",
                    description = if (searchQuery.isNotEmpty()) "No inventory items match \"$searchQuery\"." else "Tap 'Add New Mine' to create your first inventory item.",
                    modifier = Modifier.weight(1f)
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStocks, key = { it.id }) { stock ->
                        StockGridCard(
                                stock,
                                
                                onClick = {
                                    haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                    .LongPress
                                    )
                                    onNavigateToStockDetail(stock.id)
                                }
                        )
                    }
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                        verticalArrangement =
                                Arrangement.spacedBy(2.dp) // Spacing handled by item padding
                ) {
                    items(filteredStocks, key = { it.id }) { stock ->
                        StockListCard(
                                stock,
                                
                                onClick = {
                                    haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                    .LongPress
                                    )
                                    onNavigateToStockDetail(stock.id)
                                }
                        )
                    }
                }
            }
        }
    }

    if (showAddStockDialog) {
        AddStockDialog(
                onDismiss = { showAddStockDialog = false },
                onConfirm = { name, weight, warehouse ->
                    viewModel.addStockEntry(mine = name, weight = weight, warehouse = warehouse)
                    showAddStockDialog = false
                }
        )
    }
}

@Composable
fun StockListCard(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Outlined.Layers,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                        stock.mineName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Text(
                        "Coal Type",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                        "${DecimalFormat("#,###.##").format(stock.totalWeight)} t",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                )
                Text(
                        "In Stock",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun StockGridCard(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(160.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                    modifier =
                            Modifier.size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Outlined.Layers,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                        stock.mineName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                            DecimalFormat("#,###.##").format(stock.totalWeight),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                    )
                    Text(
                            " tons",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddStockDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val isFormValid = name.isNotBlank()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    IOSAlertDialog(
            onDismissRequest = onDismiss,
            title = "Initialize Mine",
            message = "Setup a new coal mine or coal type repository with its first entry.",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumInput(
                            label = "Mine Name (e.g. Awan Mine)",
                            value = name,
                            onValueChange = { name = it }
                    )
                    PremiumInput(
                            label = "Initial Weight (Tons) - Optional",
                            value = weight,
                            onValueChange = { weight = it },
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                }
            },
            buttons = {
                IOSDialogButton(text = "Cancel", onClick = onDismiss)
                IOSDialogButton(
                        text = "Initialize",
                        color = if (isFormValid) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        isLast = true,
                        onClick = {
                            if (isFormValid) {
                                haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType
                                                .LongPress
                                )
                                val finalWeight = weight.toDoubleOrNull() ?: 0.0
                                onConfirm(name, finalWeight, "Initial Stock")
                            }
                        }
                )
            }
    )
}
