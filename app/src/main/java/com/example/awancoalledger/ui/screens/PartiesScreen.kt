package com.example.awancoalledger.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.Party
import com.example.awancoalledger.data.PartyType
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.components.bounceClick
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.features.PartiesViewModel
import java.util.*
import kotlin.math.absoluteValue

enum class PartySortOrder {
    NAME,
    BALANCE,
    TYPE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(viewModel: PartiesViewModel, onNavigateToLedger: (Int) -> Unit) {
    val allPartiesWithDetails by viewModel.allPartiesWithDetails.collectAsState()

    var showAddPartySheet by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<Party?>(null) }
    var partyToDelete by remember { mutableStateOf<Party?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Contacts") }

    // Sort and Grid State
    var sortOrder by remember { mutableStateOf(PartySortOrder.NAME) }
    val isGridView by viewModel.isGridView.collectAsState()
    val countryConfig by viewModel.countryConfig.collectAsState()

    val haptic = LocalHapticFeedback.current

    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Parties List/Grid
    val filteredAndSortedParties =
            allPartiesWithDetails
                    .filter {
                        val matchesSearch =
                                it.party.name.contains(searchQuery, ignoreCase = true)
                        val matchesFilter =
                                when (selectedFilter) {
                                    "Buyers" -> it.party.type == PartyType.BUYER
                                    "Suppliers" -> it.party.type == PartyType.SUPPLIER
                                    else -> true
                                }
                        matchesSearch && matchesFilter
                    }
                    .sortedWith { a, b ->
                        when (sortOrder) {
                            PartySortOrder.NAME ->
                                    a.party
                                            .name
                                            .lowercase()
                                            .compareTo(b.party.name.lowercase())
                            PartySortOrder.BALANCE -> {
                                val balA = viewModel.getBalance(a).absoluteValue
                                val balB = viewModel.getBalance(b).absoluteValue
                                balB.compareTo(balA) // Descending absolute balance
                            }
                            PartySortOrder.TYPE ->
                                    a.party.type.name.compareTo(b.party.type.name)
                        }
                    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isGridView && filteredAndSortedParties.isNotEmpty()) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    PartiesHeader(
                        backDispatcher = backDispatcher,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        showAddPartySheet = { showAddPartySheet = true },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        sortOrder = sortOrder,
                        onSortChange = { sortOrder = it },
                        isGridView = isGridView,
                        onGridToggle = { viewModel.toggleGridView(it) },
                        haptic = haptic
                    )
                }
                items(filteredAndSortedParties, key = { it.party.id }) { details ->
                    PartyGridItem(
                            party = details.party,
                            balance = viewModel.getBalance(details),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToLedger(details.party.id)
                            }
                    )
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize()
            ) {
                item {
                    PartiesHeader(
                        backDispatcher = backDispatcher,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        showAddPartySheet = { showAddPartySheet = true },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        sortOrder = sortOrder,
                        onSortChange = { sortOrder = it },
                        isGridView = isGridView,
                        onGridToggle = { viewModel.toggleGridView(it) },
                        haptic = haptic
                    )
                }

                if (filteredAndSortedParties.isEmpty()) {
                    item {
                        com.example.awancoalledger.ui.components.EmptyStateCard(
                            icon = Icons.Outlined.Contacts,
                            title = if (searchQuery.isNotEmpty()) "No Results" else "No Contacts Yet",
                            description = if (searchQuery.isNotEmpty()) "No contacts match \"$searchQuery\"." else "Add your first contact to start tracking.",
                            actionText = if (searchQuery.isEmpty()) "Add Your First Party" else null,
                            onAction = if (searchQuery.isEmpty()) { { showAddPartySheet = true } } else null,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                } else {
                    itemsIndexed(filteredAndSortedParties, key = { _, it -> it.party.id }) { index, details ->
                        val isFirst = index == 0
                        val isLast = index == filteredAndSortedParties.size - 1
                        val shape = when {
                            isFirst && isLast -> RoundedCornerShape(32.dp)
                            isFirst -> RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            isLast -> RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                            else -> RectangleShape
                        }

                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val balance = remember(details) { viewModel.getBalance(details) }
                            SwipeableItem(
                                    onEdit = { editingParty = details.party },
                                    onDelete = { partyToDelete = details.party },
                                    shape = shape,
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    content = {
                                        PartyListItem(
                                                party = details.party,
                                                balance = balance,
                                                onClick = {
                                                    haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                    )
                                                    onNavigateToLedger(details.party.id)
                                                },
                                                showDivider = !isLast
                                        )
                                    },
                            )
                        }
                    }
                }
            }
        }

        if (showAddPartySheet) {
            PartyActionSheet(
                    countryConfig = countryConfig,
                    onDismiss = { showAddPartySheet = false },
                    onAction = { name, phone, address, type ->
                        viewModel.addParty(name, phone, address, type)
                        showAddPartySheet = false
                    }
            )
        }

        editingParty?.let { party ->
            PartyActionSheet(
                    party = party,
                    countryConfig = countryConfig,
                    onDismiss = { editingParty = null },
                    onAction = { name, phone, address, type ->
                        viewModel.updateParty(
                                party.copy(
                                        name = name,
                                        phone = phone,
                                        address = address,
                                        type = type
                                )
                        )
                        editingParty = null
                    }
            )
        }

        partyToDelete?.let { party ->
            DeleteConfirmationDialog(
                    title = "Delete Party",
                    message =
                            "Are you sure you want to delete ${party.name}? All history will be removed.",
                    onConfirm = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteParty(party)
                        partyToDelete = null
                    },
                    onDismiss = { partyToDelete = null }
            )
        }
    }
}

@Composable
fun PartyFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
            onClick = onClick,
            color =
                    if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(32.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                    label,
                    color =
                            if (selected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PartyGridItem(party: Party, balance: Double, onClick: () -> Unit) {
    val isBuyer = party.type == PartyType.BUYER
    val isReceivable = if (isBuyer) balance >= 0 else balance < 0
    val statusColor =
            if (balance == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
            else if (isReceivable) SuccessGreen else ErrorRed
    val statusText =
            if (balance == 0.0) "Cleared" else if (isReceivable) "Receivable" else "Payable"

    Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                            if (party.type == PartyType.BUYER)
                                                    MaterialTheme.colorScheme.primary
                                            else iOSOrange
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        if (party.type == PartyType.BUYER) Icons.Outlined.Person
                        else Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    party.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    if (balance == 0.0) "Cleared"
                    else
                            "Rs. ${String.format(Locale.getDefault(), "%,.0f", balance.absoluteValue)}",
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
            )
            Text(
                    statusText,
                    color = statusColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PartyCardItem(party: Party, balance: Double, onClick: () -> Unit) {
    val isBuyer = party.type == PartyType.BUYER
    val isReceivable = if (isBuyer) balance >= 0 else balance < 0
    val statusColor =
            if (balance == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
            else if (isReceivable) SuccessGreen else ErrorRed
    val statusText =
            if (balance == 0.0) "Cleared" else if (isReceivable) "Receivable" else "Payable"

    Row(
            modifier = Modifier.fillMaxWidth().bounceClick { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                        if (party.type == PartyType.BUYER)
                                                MaterialTheme.colorScheme.primary
                                        else iOSOrange
                                ),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    party.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    party.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                        color =
                                (if (party.type == PartyType.BUYER) MaterialTheme.colorScheme.primary else iOSOrange)
                                        .copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                            party.type.name.uppercase(),
                            color = if (party.type == PartyType.BUYER) MaterialTheme.colorScheme.primary else iOSOrange,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        party.phone,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                    if (balance == 0.0) "Cleared"
                    else
                            "Rs. ${String.format(Locale.getDefault(), "%,.0f", balance.absoluteValue)}",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
            )
            Text(
                    statusText,
                    color = statusColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyActionSheet(
        party: Party? = null,
        countryConfig: com.example.awancoalledger.data.CountryConfig,
        onDismiss: () -> Unit,
        onAction: (String, String, String, PartyType) -> Unit
) {
    var name by remember { mutableStateOf(party?.name ?: "") }
    // Store only digits after prefix
    var phoneDigits by remember { mutableStateOf(party?.phone?.removePrefix(countryConfig.code) ?: "") }
    var address by remember { mutableStateOf(party?.address ?: "") }
    var type by remember { mutableStateOf(party?.type ?: PartyType.BUYER) }

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }
                Text(
                        if (party == null) "New Party" else "Edit Party",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                        onClick = {
                            if (name.isNotBlank()) onAction(name, "${countryConfig.code}$phoneDigits", address, type)
                        }
                ) {
                    Text(
                            if (party == null) "Add" else "Save",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumInput(label = "Name", value = name, onValueChange = { name = it })

                PremiumInput(
                        label = "Phone Number",
                        value = phoneDigits,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= countryConfig.maxDigits) {
                                phoneDigits = input
                            }
                        },
                        prefix = {
                            Text(
                                    "${countryConfig.code} ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                            )
                        },
                        keyboardType = KeyboardType.Number
                )

                PremiumInput(label = "Address", value = address, onValueChange = { address = it })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                    "PARTY TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f
                                            )
                                    )
                                    .padding(2.dp)
            ) {
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                                if (type == PartyType.BUYER)
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                                else Color.Transparent
                                        )
                                        .clickable { type = PartyType.BUYER },
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            "Buyer",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight =
                                    if (type == PartyType.BUYER) FontWeight.Bold
                                    else FontWeight.Normal
                    )
                }
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                                if (type == PartyType.SUPPLIER)
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                                else Color.Transparent
                                        )
                                        .clickable { type = PartyType.SUPPLIER },
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            "Supplier",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight =
                                    if (type == PartyType.SUPPLIER) FontWeight.Bold
                                    else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun PartiesHeader(
    backDispatcher: androidx.activity.OnBackPressedDispatcher?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showAddPartySheet: () -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    sortOrder: PartySortOrder,
    onSortChange: (PartySortOrder) -> Unit,
    isGridView: Boolean,
    onGridToggle: (Boolean) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        com.example.awancoalledger.ui.components.ScreenHeader(
            title = "Contacts",
            onBack = { backDispatcher?.onBackPressed() },
            modifier = Modifier.padding(horizontal = 0.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        com.example.awancoalledger.ui.components.PremiumInput(
                label = "Search Contacts",
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = {
                    Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add Contact Button
        Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddPartySheet()
                },
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background
                        ),
                shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        "Add New Party",
                        color = MaterialTheme.colorScheme.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Filter Tabs + Sort/Grid Buttons
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PartyFilterPill("All Contacts", selectedFilter == "All Contacts") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChange("All Contacts")
                }
                PartyFilterPill("Buyers", selectedFilter == "Buyers") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChange("Buyers")
                }
                PartyFilterPill("Suppliers", selectedFilter == "Suppliers") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChange("Suppliers")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                        onClick = {
                            val newOrder = when (sortOrder) {
                                        PartySortOrder.NAME -> PartySortOrder.BALANCE
                                        PartySortOrder.BALANCE -> PartySortOrder.TYPE
                                        PartySortOrder.TYPE -> PartySortOrder.NAME
                                    }
                            onSortChange(newOrder)
                        }
                ) {
                    Icon(
                            Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "Sort",
                            tint =
                                    if (sortOrder != PartySortOrder.NAME) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onGridToggle(!isGridView) }) {
                    Icon(
                            imageVector =
                                    if (isGridView) Icons.Outlined.List
                                    else Icons.Outlined.GridView,
                            contentDescription = "Toggle View",
                            tint =
                                    if (isGridView) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
                "CONTACTS (${sortOrder.name})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun PartyListItem(
    party: com.example.awancoalledger.data.Party,
    balance: Double,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val isBuyer = party.type == com.example.awancoalledger.data.PartyType.BUYER
    val isReceivable = if (isBuyer) balance >= 0 else balance < 0
    val statusColor = if (balance == 0.0) MaterialTheme.colorScheme.primary else if (isReceivable) com.example.awancoalledger.ui.theme.SuccessGreen else com.example.awancoalledger.ui.theme.ErrorRed
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (party.type == com.example.awancoalledger.data.PartyType.BUYER) MaterialTheme.colorScheme.primary else com.example.awancoalledger.ui.theme.iOSOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (party.type == com.example.awancoalledger.data.PartyType.BUYER) Icons.Outlined.Person else Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(party.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(party.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (balance == 0.0) "Cleared" else "Rs. ${String.format(java.util.Locale.getDefault(), "%,.0f", kotlin.math.abs(balance))}",
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(if (balance == 0.0) "" else if (isReceivable) "Receivable" else "Payable", fontSize = 11.sp, color = statusColor.copy(alpha = 0.8f))
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

