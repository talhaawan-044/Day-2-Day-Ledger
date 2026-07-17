package com.example.awancoalledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.FuelEntry
import com.example.awancoalledger.data.MaintenanceEntry
import com.example.awancoalledger.data.Vehicle
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VehicleTrackerScreen(viewModel: LedgerViewModel, onNavigateBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    
    val vehicles by viewModel.allVehicles.collectAsState()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsState()
    
    val fuelEntries by viewModel.allFuelEntries.collectAsState()
    val maintenanceEntries by viewModel.allMaintenanceEntries.collectAsState()
    val avgKmPerLiter by viewModel.avgKmPerLiter.collectAsState()
    val nextOilChange by viewModel.nextOilChangeMileage.collectAsState()
    val kmsRemaining by viewModel.kmsRemainingForOilChange.collectAsState()
    val currentMileage by viewModel.currentVehicleMileage.collectAsState()
    val alertMessage by viewModel.vehicleAlert.collectAsState()
    
    val monthlyFuelCost by viewModel.monthlyFuelCost.collectAsState()
    val monthlyMaintenanceCost by viewModel.monthlyMaintenanceCost.collectAsState()
    val efficiencyTrend by viewModel.fuelEfficiencyTrend.collectAsState()
    
    var showLogModal by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<Any?>(null) }
    var logToDelete by remember { mutableStateOf<Any?>(null) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fuel, 1: Oil Change, 2: Services
    
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { vehicles.size + 1 })

    // Sync selected vehicle with pager
    LaunchedEffect(pagerState.currentPage, vehicles) {
        if (pagerState.currentPage < vehicles.size) {
            viewModel.selectVehicle(vehicles[pagerState.currentPage].id)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- iOS Premium Header ---
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                com.example.awancoalledger.ui.components.ScreenHeader(
                    title = "Garage",
                    onBack = { onNavigateBack() },
                    actions = {
                        IconButton(onClick = { showAddVehicle = true }) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    // --- Vehicle Carousel ---
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) { page ->
                        val isSelected = pagerState.currentPage == page
                        val scale by animateFloatAsState(if (isSelected) 1f else 0.9f)
                        
                        if (page < vehicles.size) {
                            val vehicle = vehicles[page]
                            var showEditModal by remember { mutableStateOf(false) }
                            
                            VehicleCard(
                                vehicle = vehicle,
                                isSelected = isSelected,
                                onEdit = { showEditModal = true },
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )

                            if (showEditModal) {
                                EditVehicleModal(
                                    vehicle = vehicle,
                                    onDismiss = { showEditModal = false },
                                    onUpdate = { viewModel.updateVehicle(it) },
                                    onDelete = { viewModel.deleteVehicle(vehicle) }
                                )
                            }
                        } else {
                            AddVehiclePlaceholder(
                                onClick = { showAddVehicle = true },
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                        }
                    }
                }

                if (vehicles.isEmpty() || pagerState.currentPage >= vehicles.size) {
                    item {
                        com.example.awancoalledger.ui.components.EmptyStateCard(
                            icon = Icons.Outlined.DirectionsCar,
                            title = "No Vehicle Selected",
                            description = "Add a vehicle to start tracking logs"
                        )
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            // --- Hero Maintenance Card ---
                            PremiumMaintenanceCard(nextOilChange, kmsRemaining)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // --- Quick Actions ---
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ActionButton(
                                    label = "Add Entry",
                                    icon = Icons.Outlined.AddCircle,
                                    color = PrimaryBlue,
                                    onClick = { 
                                        logToEdit = null
                                        showLogModal = true 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text("KEY METRICS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                            Spacer(Modifier.height(12.dp))

                            // --- Stats Grid ---
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStatItem(
                                    modifier = Modifier.weight(1f),
                                    label = "Monthly Fuel",
                                    value = String.format("Rs. %,.0f", monthlyFuelCost),
                                    icon = Icons.Outlined.CurrencyExchange,
                                    color = SuccessGreen
                                )
                                TrackerStatItem(
                                    modifier = Modifier.weight(1f),
                                    label = "Service Cost",
                                    value = String.format("Rs. %,.0f", monthlyMaintenanceCost),
                                    icon = Icons.Outlined.Handyman,
                                    color = iOSOrange
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStatItem(
                                    modifier = Modifier.weight(1f),
                                    label = "Fuel Economy",
                                    value = String.format("%.1f km/L", avgKmPerLiter),
                                    icon = Icons.Outlined.AutoGraph,
                                    color = PrimaryBlue
                                )
                                TrackerStatItem(
                                    modifier = Modifier.weight(1f),
                                    label = "Total Run",
                                    value = String.format("%,.0f km", currentMileage),
                                    icon = Icons.Outlined.Timeline,
                                    color = iOSPurple
                                )
                            }
                            
                            if (efficiencyTrend.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("EFFICIENCY TREND", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 0.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(modifier = Modifier.padding(20.dp)) {
                                        PremiumLineGraph(
                                            data = efficiencyTrend,
                                            lineColor = PrimaryBlue,
                                            fillColor = PrimaryBlue.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // --- iOS Segmented Control ---
                            VehicleSegmentedControl(selectedTab) { selectedTab = it }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    val filteredEntries = when (selectedTab) {
                        0 -> fuelEntries.map { it to "FUEL" }
                        1 -> maintenanceEntries.filter { it.type == "OIL_CHANGE" }.map { it to "MAINTENANCE" }
                        2 -> maintenanceEntries.filter { it.type != "OIL_CHANGE" }.map { it to "MAINTENANCE" }
                        else -> (fuelEntries.map { it to "FUEL" } + maintenanceEntries.map { it to "MAINTENANCE" })
                    }.sortedByDescending { pair ->
                        val entry = pair.first
                        if (entry is FuelEntry) entry.date else (entry as MaintenanceEntry).date
                    }
                    
                    if (filteredEntries.isEmpty()) {
                        item {
                            com.example.awancoalledger.ui.components.EmptyStateCard(
                                icon = Icons.Outlined.History,
                                title = "No Logs Found",
                                description = "No fuel or maintenance logs found for this vehicle."
                            )
                        }
                    } else {
                        // Group by Month Year
                        val grouped = filteredEntries.groupBy { pair ->
                            val date = if (pair.first is FuelEntry) (pair.first as FuelEntry).date else (pair.first as MaintenanceEntry).date
                            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(date))
                        }
                        
                        grouped.forEach { (month, entries) ->
                            stickyHeader {
                                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(month.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                                }
                            }
                            
                            items(entries) { (entry, type) ->
                                SwipeableItem(
                                    onDelete = { logToDelete = entry },
                                    onEdit = { 
                                        logToEdit = entry
                                        showLogModal = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    VehicleLogItem(entry, type)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- Modals ---
        if (alertMessage != null) {
            IOSAlertDialog(
                onDismissRequest = { viewModel.dismissVehicleAlert() },
                title = "Vehicle Alert",
                message = alertMessage!!,
                buttons = {
                    IOSDialogButton(text = "OK", fontWeight = FontWeight.Bold, isLast = true, onClick = { viewModel.dismissVehicleAlert() })
                }
            )
        }
        
        if (showLogModal) {
            LogEntryModal(
                entry = logToEdit,
                onDismiss = { 
                    showLogModal = false
                    logToEdit = null
                },
                onAddFuel = { m, l, a, d -> viewModel.addFuelEntry(m, l, a, d) },
                onAddService = { m, c, d, i, dt -> viewModel.addMaintenanceEntry(m, c, d, i, dt) },
                onUpdateFuel = { viewModel.updateFuelEntry(it) },
                onUpdateService = { viewModel.updateMaintenanceEntry(it) }
            )
        }

        if (logToDelete != null) {
            val entry = logToDelete!!
            val title = if (entry is FuelEntry) "Delete Fuel Entry" else "Delete Service Log"
            val message = if (entry is FuelEntry) "Remove this fuel purchase of ${entry.liters}L?" else "Remove this service log?"
            
            com.example.awancoalledger.ui.components.DeleteConfirmationDialog(
                title = title,
                message = message,
                onConfirm = {
                    if (entry is FuelEntry) viewModel.deleteFuelEntry(entry)
                    else if (entry is MaintenanceEntry) viewModel.deleteMaintenanceEntry(entry)
                    logToDelete = null
                },
                onDismiss = { logToDelete = null }
            )
        }

        if (showAddVehicle) {
            AddVehicleModal(
                onDismiss = { showAddVehicle = false },
                onAdd = { n, p, t, m, pr -> viewModel.addVehicle(n, p, t, m, pr) }
            )
        }
    }
}

@Composable
fun VehicleCard(vehicle: Vehicle, isSelected: Boolean, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(180.dp),
        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isSelected) 8.dp else 2.dp,
        border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative background pattern
            Icon(
                imageVector = if (vehicle.type == "TRUCK") Icons.Outlined.LocalShipping else Icons.Outlined.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(200.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp),
                tint = (if (isSelected) Color.White else PrimaryBlue).copy(alpha = 0.05f)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(
                                (if (isSelected) Color.White else PrimaryBlue).copy(alpha = 0.15f),
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (vehicle.type == "TRUCK") Icons.Outlined.LocalShipping else Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                vehicle.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                vehicle.plateNumber,
                                fontSize = 14.sp,
                                color = (if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vehicle.isPrimary) {
                            Surface(
                                color = (if (isSelected) Color.White else PrimaryBlue).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "PRIMARY",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else PrimaryBlue
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("LAST MILEAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = (if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f))
                        Text(
                            "${vehicle.currentMileage.toInt()} km",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddVehiclePlaceholder(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(180.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Add New Vehicle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = color
            )
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun PremiumMaintenanceCard(nextMileage: Double, kmsLeft: Double) {
    val intensityColor = when {
        kmsLeft <= 0 -> ErrorRed
        kmsLeft < 500 -> iOSOrange
        else -> SuccessGreen
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (nextMileage > 0) (kmsLeft / 3000.0).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(intensityColor, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.OilBarrel,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("OIL SERVICE STATUS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(
                            text = if (nextMileage > 0) String.format("%,.0f", nextMileage) else "---",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("TARGET ODOMETER (KM)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (kmsLeft > 0) String.format("%,.0f", kmsLeft) else "OVERDUE",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = intensityColor
                        )
                        Text("KM REMAINING", fontSize = 10.sp, fontWeight = FontWeight.Black, color = intensityColor.copy(alpha = 0.6f))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = intensityColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when {
                        nextMileage <= 0 -> "Add an oil change entry to start tracking"
                        kmsLeft <= 0 -> "Service critical! Overdue by ${(-kmsLeft).toInt()} km"
                        else -> "Your next service is scheduled at ${nextMileage.toInt()} km"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrackerStatItem(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun VehicleSegmentedControl(selectedIndex: Int, options: List<String> = listOf("Fuel", "Oil Change", "Services"), onSelect: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = if (selected) 4.dp else 0.dp
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleLogItem(entry: Any, type: String) {
    val date = if (entry is FuelEntry) entry.date else (entry as MaintenanceEntry).date
    val dateStr = SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(Date(date))
    
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(
                if (type == "FUEL") SuccessGreen.copy(alpha = 0.1f) else iOSOrange.copy(alpha = 0.1f),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (type == "FUEL") Icons.Outlined.LocalGasStation else Icons.Outlined.Handyman,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (type == "FUEL") SuccessGreen else iOSOrange
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    type == "FUEL" -> "Fuel Purchase"
                    (entry as MaintenanceEntry).type == "OIL_CHANGE" -> entry.description.ifBlank { "Oil Change" }
                    else -> entry.description.ifBlank { "General Service" }
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(dateStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (type == "FUEL") String.format("%.2f L", (entry as FuelEntry).liters) else "Rs. %,.0f".format((entry as MaintenanceEntry).cost),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = if (type == "FUEL") SuccessGreen else ErrorRed
            )
            Text("${if (type == "FUEL") (entry as FuelEntry).mileage.toInt() else (entry as MaintenanceEntry).mileage.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleModal(onDismiss: () -> Unit, onAdd: (String, String, String, Double, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TRUCK") }
    var mileage by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Add New Vehicle", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumInput(label = "Vehicle Nickname (e.g. Blue Hino)", value = name, onValueChange = { name = it })
            Spacer(Modifier.height(12.dp))
            PremiumInput(label = "Plate Number", value = plate, onValueChange = { plate = it })
            Spacer(Modifier.height(12.dp))
            PremiumInput(label = "Initial Odometer (km)", value = mileage, onValueChange = { mileage = it }, keyboardType = KeyboardType.Number)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                onClick = { isPrimary = !isPrimary },
                color = if (isPrimary) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = if (isPrimary) BorderStroke(2.dp, PrimaryBlue) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue))
                    Spacer(Modifier.width(8.dp))
                    Text("Set as Primary Vehicle", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("VEHICLE TYPE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeButton("TRUCK", Icons.Outlined.LocalShipping, type == "TRUCK", Modifier.weight(1f)) { type = "TRUCK" }
                TypeButton("CAR", Icons.Outlined.DirectionsCar, type == "CAR", Modifier.weight(1f)) { type = "CAR" }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && plate.isNotBlank()) {
                        onAdd(name, plate, type, mileage.toDoubleOrNull() ?: 0.0, isPrimary)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add Vehicle", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleModal(vehicle: Vehicle, onDismiss: () -> Unit, onUpdate: (Vehicle) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(vehicle.name) }
    var plate by remember { mutableStateOf(vehicle.plateNumber) }
    var type by remember { mutableStateOf(vehicle.type) }
    var isPrimary by remember { mutableStateOf(vehicle.isPrimary) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Edit Vehicle", fontSize = 28.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumInput(label = "Vehicle Nickname", value = name, onValueChange = { name = it })
            Spacer(Modifier.height(12.dp))
            PremiumInput(label = "Plate Number", value = plate, onValueChange = { plate = it })
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                onClick = { isPrimary = !isPrimary },
                color = if (isPrimary) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = if (isPrimary) BorderStroke(2.dp, PrimaryBlue) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue))
                    Spacer(Modifier.width(8.dp))
                    Text("Set as Primary Vehicle", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("VEHICLE TYPE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeButton("TRUCK", Icons.Outlined.LocalShipping, type == "TRUCK", Modifier.weight(1f)) { type = "TRUCK" }
                TypeButton("CAR", Icons.Outlined.DirectionsCar, type == "CAR", Modifier.weight(1f)) { type = "CAR" }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && plate.isNotBlank()) {
                        onUpdate(vehicle.copy(name = name, plateNumber = plate, type = type, isPrimary = isPrimary))
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Update Vehicle", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        com.example.awancoalledger.ui.components.DeleteConfirmationDialog(
            title = "Delete Vehicle?",
            message = "This will permanently remove the vehicle and its history.",
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun TypeButton(label: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryModal(
    entry: Any? = null,
    onDismiss: () -> Unit,
    onAddFuel: (Double, Double, Double, Long) -> Unit,
    onAddService: (Double, Double, String, Boolean, Long) -> Unit,
    onUpdateFuel: (FuelEntry) -> Unit,
    onUpdateService: (MaintenanceEntry) -> Unit
) {
    var mode by remember { 
        mutableIntStateOf(
            when {
                entry is FuelEntry -> 0
                entry is MaintenanceEntry && entry.type == "OIL_CHANGE" -> 1
                entry is MaintenanceEntry -> 2
                else -> 0
            }
        )
    }
    
    var mileage by remember { mutableStateOf(if (entry is FuelEntry) entry.mileage.toInt().toString() else if (entry is MaintenanceEntry) entry.mileage.toInt().toString() else "") }
    var liters by remember { mutableStateOf(if (entry is FuelEntry) entry.liters.toString() else "") }
    var amount by remember { mutableStateOf(if (entry is FuelEntry) entry.amount.toInt().toString() else if (entry is MaintenanceEntry) entry.cost.toInt().toString() else "") }
    var desc by remember { mutableStateOf(if (entry is MaintenanceEntry) entry.description else "") }
    var selectedDate by remember { mutableLongStateOf(if (entry is FuelEntry) entry.date else if (entry is MaintenanceEntry) entry.date else System.currentTimeMillis()) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            val title = if (entry != null) "Edit Entry" else when(mode) {
                0 -> "Fuel Refill"
                1 -> "Oil Change"
                else -> "Service Log"
            }
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (entry == null) {
                VehicleSegmentedControl(
                    selectedIndex = mode,
                    options = listOf("Fuel", "Oil Change", "Services")
                ) { mode = it }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // --- Date Picker Trigger ---
            Surface(
                onClick = { showDatePicker = true },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Entry Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)), fontWeight = FontWeight.Black, color = PrimaryBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Layout based on mode
            when (mode) {
                0 -> { // Fuel
                    PremiumInput(label = "Current Odometer (km)", value = mileage, onValueChange = { mileage = it }, keyboardType = KeyboardType.Number)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { PremiumInput(label = "Liters", value = liters, onValueChange = { liters = it }, keyboardType = KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { PremiumInput(label = "Cost (Rs.)", value = amount, onValueChange = { amount = it }, keyboardType = KeyboardType.Number) }
                    }
                }
                1 -> { // Oil Change
                    PremiumInput(label = "Current Odometer (km)", value = mileage, onValueChange = { mileage = it }, keyboardType = KeyboardType.Number)
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumInput(label = "Price (Rs.)", value = amount, onValueChange = { amount = it }, keyboardType = KeyboardType.Number)
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumInput(label = "Description", value = desc, onValueChange = { desc = it })
                }
                2 -> { // Services
                    if (entry is MaintenanceEntry && entry.type != "OIL_CHANGE") {
                         PremiumInput(label = "Current Odometer (km)", value = mileage, onValueChange = { mileage = it }, keyboardType = KeyboardType.Number)
                         Spacer(modifier = Modifier.height(12.dp))
                    }
                    PremiumInput(label = "Total Service Cost (Rs.)", value = amount, onValueChange = { amount = it }, keyboardType = KeyboardType.Number)
                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumInput(label = "What was done? (Description)", value = desc, onValueChange = { desc = it })
                }
            }

            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val m = mileage.toDoubleOrNull() ?: 0.0
                    val a = amount.toDoubleOrNull() ?: 0.0
                    val l = liters.toDoubleOrNull() ?: 0.0
                    
                    if (entry != null) {
                        if (entry is FuelEntry) {
                            onUpdateFuel(entry.copy(mileage = m, liters = l, amount = a, date = selectedDate, lastUpdated = System.currentTimeMillis()))
                        } else if (entry is MaintenanceEntry) {
                            onUpdateService(entry.copy(mileage = m, cost = a, description = desc, date = selectedDate, lastUpdated = System.currentTimeMillis()))
                        }
                    } else {
                        when (mode) {
                            0 -> onAddFuel(m, l, a, selectedDate)
                            1 -> onAddService(m, a, desc.ifEmpty { "Oil Change" }, true, selectedDate)
                            2 -> onAddService(m, a, desc, false, selectedDate)
                        }
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (entry != null) "Update Entry" else "Save Entry", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    
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
}

