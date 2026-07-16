package com.example.awancoalledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
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
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.LedgerViewModel
import java.util.*
import kotlin.math.absoluteValue

enum class PartySortOrder {
    NAME,
    BALANCE,
    TYPE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(viewModel: LedgerViewModel, onNavigateToLedger: (Int) -> Unit) {
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                    "Contacts",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            PremiumInput(
                    label = "Search Contacts",
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                                Icons.Default.Search,
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
                        showAddPartySheet = true
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
                            Icons.Default.PersonAdd,
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
                        selectedFilter = "All Contacts"
                    }
                    PartyFilterPill("Buyers", selectedFilter == "Buyers") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedFilter = "Buyers"
                    }
                    PartyFilterPill("Suppliers", selectedFilter == "Suppliers") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedFilter = "Suppliers"
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                            onClick = {
                                sortOrder =
                                        when (sortOrder) {
                                            PartySortOrder.NAME -> PartySortOrder.BALANCE
                                            PartySortOrder.BALANCE -> PartySortOrder.TYPE
                                            PartySortOrder.TYPE -> PartySortOrder.NAME
                                        }
                            }
                    ) {
                        Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint =
                                        if (sortOrder != PartySortOrder.NAME) PrimaryBlue
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.toggleGridView(!isGridView) }) {
                        Icon(
                                imageVector =
                                        if (isGridView) Icons.Default.List
                                        else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint =
                                        if (isGridView) PrimaryBlue
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

            if (filteredAndSortedParties.isEmpty()) {
                Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                ) { Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                    ) {
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
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .weight(
                                                    1f,
                                                    fill = false
                                            ) // Allow it to wrap if content is small
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        items(filteredAndSortedParties, key = { it.party.id }) { details ->
                            SwipeableItem(
                                    onEdit = { editingParty = details.party },
                                    onDelete = { partyToDelete = details.party },
                                    content = {
                                        PartyCardItem(
                                                party = details.party,
                                                balance = viewModel.getBalance(details),
                                                onClick = {
                                                    haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                    )
                                                    onNavigateToLedger(details.party.id)
                                                }
                                        )
                                    },
                            )
                            HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
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

        if (editingParty != null) {
            PartyActionSheet(
                    party = editingParty,
                    countryConfig = countryConfig,
                    onDismiss = { editingParty = null },
                    onAction = { name, phone, address, type ->
                        viewModel.updateParty(
                                editingParty!!.copy(
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
            modifier = Modifier.fillMaxWidth().height(160.dp)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Box(
                    modifier =
                            Modifier.size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                            if (party.type == PartyType.BUYER)
                                                    PrimaryBlue.copy(alpha = 0.15f)
                                            else iOSOrange.copy(alpha = 0.15f)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        party.name.take(1).uppercase(),
                        color = if (party.type == PartyType.BUYER) PrimaryBlue else iOSOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(
                                        if (party.type == PartyType.BUYER)
                                                PrimaryBlue.copy(alpha = 0.2f)
                                        else iOSOrange.copy(alpha = 0.2f)
                                ),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    party.name.take(1).uppercase(),
                    color = if (party.type == PartyType.BUYER) PrimaryBlue else iOSOrange,
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
                                (if (party.type == PartyType.BUYER) PrimaryBlue else iOSOrange)
                                        .copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                            party.type.name.uppercase(),
                            color = if (party.type == PartyType.BUYER) PrimaryBlue else iOSOrange,
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
                Icons.AutoMirrored.Filled.ArrowForwardIos,
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
                TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryBlue) }
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
                            color = PrimaryBlue,
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
