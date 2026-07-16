package com.example.awancoalledger.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.PartyWithDetails
import com.example.awancoalledger.data.Party
import com.example.awancoalledger.data.PartyType
import com.example.awancoalledger.data.LedgerEntry
import com.example.awancoalledger.data.Payment
import com.example.awancoalledger.data.PaymentType
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.LedgerViewModel
import com.example.awancoalledger.utils.ExportUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// Helper to format date globally as requested: "April 19, 2026"
private fun formatDisplayDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

private fun formatFullDateWithDay(dateMillis: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDetailScreen(
    viewModel: LedgerViewModel,
    partyId: Int,
    onBack: () -> Unit
) {
    val detailsState: State<PartyWithDetails?> = viewModel.partyDetails.collectAsState(initial = null)
    val details: PartyWithDetails? = detailsState.value
    val countryConfig by viewModel.countryConfig.collectAsState()
    
    // Preview States
    var previewingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var previewingPayment by remember { mutableStateOf<Payment?>(null) }
    var showPartyPreview by remember { mutableStateOf(false) }

    // Edit States
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var editingPayment by remember { mutableStateOf<Payment?>(null) }
    var editingParty by remember { mutableStateOf<Party?>(null) }
    
    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showAddPaymentSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    
    // Deletion states
    var entryToDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (details == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    val balance = viewModel.getBalance(details)
    val isBuyer = details.party.type == PartyType.BUYER
    
    // For Buyer, positive balance means they owe us (Receivable)
    // For Supplier, positive balance means we owe them (Payable)
    val isReceivable = if (isBuyer) balance >= 0 else balance < 0
    val statusColor = if (isReceivable) SuccessGreen else ErrorRed
    val statusText = if (isReceivable) "RECEIVABLE" else "PAYABLE"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        details.party.name, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        details.party.type.name, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Invisible spacer to balance the back button
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Balance Card Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NET BALANCE", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Rs.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp, end = 4.dp))
                        Text(String.format(Locale.getDefault(), "%,.0f", balance.absoluteValue), color = MaterialTheme.colorScheme.onSurface, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Text(
                            statusText, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Content
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Quick Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LargeActionButton(
                            label = "New Entry", 
                            icon = Icons.Default.Add, 
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddEntrySheet = true 
                            }
                        )
                        LargeActionButton(
                            label = "Payment", 
                            icon = Icons.Default.Add, 
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddPaymentSheet = true 
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryActionButton(
                            label = "Share Ledger", 
                            icon = Icons.Default.Share, 
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExportSheet = true 
                            }
                        )
                        
                        SecondaryActionButton(
                            label = "Party Info", 
                            icon = Icons.Default.Info, 
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPartyPreview = true 
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // History List with Running Balances
                    val chronologicalItems = (details.entries.map { it to "entry" } + details.payments.map { it to "payment" })
                        .sortedBy { pair ->
                            val item = pair.first
                            if (item is LedgerEntry) item.date else (item as Payment).date
                        }

                    var runningBalance = 0.0
                    val balancesChronological = mutableListOf<Double>()
                    chronologicalItems.forEach { (item, type) ->
                        if (type == "entry") {
                            val entry = item as LedgerEntry
                            val bill = ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
                            runningBalance += if (details.party.type == PartyType.BUYER) bill else -bill
                        } else {
                            val p = item as Payment
                            val isIncoming = p.type == PaymentType.THEY_PAID
                            val isNegativeEffect = if (details.party.type == PartyType.BUYER) isIncoming else !isIncoming
                            runningBalance += if (isNegativeEffect) -p.amount else p.amount
                        }
                        balancesChronological.add(runningBalance)
                    }

                    val historyItemsWithBalance = chronologicalItems.zip(balancesChronological).reversed()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItemsWithBalance, key = { (pair, _) -> 
                            val (item, type) = pair
                            if (type == "entry") "e_${(item as LedgerEntry).id}" 
                            else "p_${(item as Payment).id}" 
                        }) { (pair, bal) ->
                            val (item, type) = pair
                            SwipeableItem(
                                onEdit = { 
                                    if (type == "entry") editingEntry = item as LedgerEntry 
                                    else editingPayment = item as Payment 
                                },
                                onDelete = { 
                                    if (type == "entry") entryToDelete = item as LedgerEntry 
                                    else paymentToDelete = item as Payment 
                                },
                                shape = RoundedCornerShape(24.dp),
                                backgroundColor = MaterialTheme.colorScheme.surface
                            ) {
                                if (type == "entry") {
                                    HistoryCardItem(item as LedgerEntry, details.party.type, bal) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        previewingEntry = item
                                    }
                                } else {
                                    PaymentCardItem(item as Payment, details.party.type, bal) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        previewingPayment = item
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preview Sheets
        previewingEntry?.let { entry ->
            EntryPreviewSheet(
                entry = entry,
                partyType = details.party.type,
                onDismiss = { previewingEntry = null },
                onAction = {
                    editingEntry = entry
                    previewingEntry = null
                },
                onShare = { com.example.awancoalledger.utils.ExportUtils.shareDetailedEntry(context, entry) }
            )
        }

        previewingPayment?.let { payment ->
            PaymentPreviewSheet(
                payment = payment,
                partyType = details.party.type,
                onDismiss = { previewingPayment = null },
                onAction = {
                    editingPayment = payment
                    previewingPayment = null
                },
                onShare = { com.example.awancoalledger.utils.ExportUtils.shareDetailedPayment(context, payment) }
            )
        }

        if (showExportSheet && details != null) {
            ExportActionSheet(
                viewModel = viewModel,
                details = details,
                balance = balance,
                onDismiss = { showExportSheet = false }
            )
        }

        if (showPartyPreview && details != null) {
            PartyPreviewSheet(
                party = details.party,
                onDismiss = { showPartyPreview = false },
                onEdit = {
                    editingParty = details.party
                    showPartyPreview = false
                }
            )
        }

        // Action Sheets
        if (showAddEntrySheet) {
            EntryActionSheet(
                onDismiss = { showAddEntrySheet = false },
                onAdd = { date, truck, mine, warehouse, weight, rate, fare, adv ->
                    viewModel.addEntry(partyId, truck, mine, warehouse, weight, rate, fare, adv, date)
                    showAddEntrySheet = false
                }
            )
        }

        if (editingEntry != null) {
            EntryActionSheet(
                entry = editingEntry,
                onDismiss = { editingEntry = null },
                onAdd = { date, truck, mine, warehouse, weight, rate, fare, adv ->
                    viewModel.updateEntry(editingEntry!!.copy(
                        date = date,
                        truckNumber = truck, mine = mine, warehouse = warehouse,
                        weight = weight, rate = rate, fare = fare, advPayment = adv
                    ))
                    editingEntry = null
                }
            )
        }

        if (showAddPaymentSheet) {
            PaymentActionSheet(
                partyType = details?.party?.type ?: PartyType.BUYER,
                onDismiss = { showAddPaymentSheet = false },
                onAdd = { date, amount, type, note ->
                    viewModel.addPayment(partyId, amount, type, note, date)
                    showAddPaymentSheet = false
                }
            )
        }

        if (editingPayment != null) {
            PaymentActionSheet(
                payment = editingPayment,
                partyType = details?.party?.type ?: PartyType.BUYER,
                onDismiss = { editingPayment = null },
                onAdd = { date, amount, type, note ->
                    viewModel.updatePayment(editingPayment!!.copy(date = date, amount = amount, type = type, note = note))
                    editingPayment = null
                }
            )
        }

        if (editingParty != null) {
            PartyActionSheet(
                party = editingParty,
                countryConfig = countryConfig,
                onDismiss = { editingParty = null },
                onAction = { name, phone, address, type ->
                    viewModel.updateParty(editingParty!!.copy(name = name, phone = phone, address = address, type = type))
                    editingParty = null
                }
            )
        }

        // Delete Dialogs
        entryToDelete?.let { entry ->
            DeleteConfirmationDialog(
                title = "Delete Entry",
                message = "Are you sure you want to delete this entry for truck ${entry.truckNumber}?",
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteEntry(entry)
                    entryToDelete = null
                },
                onDismiss = { entryToDelete = null }
            )
        }

        paymentToDelete?.let { payment ->
            DeleteConfirmationDialog(
                title = "Delete Payment",
                message = "Are you sure you want to delete this payment of Rs. ${String.format(Locale.getDefault(), "%,.0f", payment.amount)}?",
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deletePayment(payment)
                    paymentToDelete = null
                },
                onDismiss = { paymentToDelete = null }
            )
        }
    }
}

@Composable
fun LargeActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SecondaryActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = PrimaryBlue, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HistoryCardItem(entry: LedgerEntry, partyType: PartyType, balance: Double, onClick: () -> Unit) {
    val total = ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
    val isBuyer = partyType == PartyType.BUYER
    
    // For Buyer, it increases Receivable (+). For Supplier, it increases Payable (+).
    val displayColor = if (isBuyer) iOSOrange else ErrorRed
    val entryColor = PrimaryBlue // Define entryColor locally for entries
    val df = DecimalFormat("#,###.##")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(entryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = entryColor, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.truckNumber ?: "Entry", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("Mine: ${entry.mine ?: "N/A"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (entry.fare != null && entry.fare > 0.0) {
                    Spacer(Modifier.height(2.dp))
                    Text("Fare: Rs. ${df.format(entry.fare)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Text(formatDisplayDate(entry.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Rs. ${df.format(total)}", color = displayColor, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("${entry.weight ?: 0.0} tons", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Bal: Rs. ${df.format(balance.absoluteValue)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaymentCardItem(payment: Payment, partyType: PartyType, balance: Double, onClick: () -> Unit) {
    val isIncoming = payment.type == PaymentType.THEY_PAID
    val label = if (isIncoming) "Payment Received" else "Payment Sent"
    val isBuyer = partyType == PartyType.BUYER

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SuccessGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!payment.note.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("Note: ${payment.note}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Text(formatDisplayDate(payment.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                // Determine effect on balance
                // Buyer: Received (-) Sent (+)
                // Supplier: Sent (-) Received (+)
                val isNegativeEffect = if (isBuyer) isIncoming else !isIncoming
                val sign = if (isNegativeEffect) "-" else "+"
                val color = if (isNegativeEffect) SuccessGreen else ErrorRed
                
                Text("$sign${String.format(Locale.getDefault(), "%,.0f", payment.amount)}", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bal: Rs. ${String.format(Locale.getDefault(), "%,.0f", balance.absoluteValue)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --- High-Fidelity Preview Sheets (Old App Style) ---

// --- Shared Preview Sheets Used ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyPreviewSheet(party: Party, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Party Information", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(16.dp)) {
                PreviewRow("Name", party.name)
                PreviewRow("Phone", party.phone)
                PreviewRow("Address", party.address)
                PreviewRow("Type", party.type.name)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Party Info", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PreviewRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

// --- Action Sheets (With Date Selection) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryActionSheet(
    entry: LedgerEntry? = null,
    onDismiss: () -> Unit,
    onAdd: (Long, String?, String?, String?, Double?, Double?, Double?, Double?) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(entry?.date ?: System.currentTimeMillis()) }
    var truck by remember { mutableStateOf(entry?.truckNumber ?: "") }
    var mine by remember { mutableStateOf(entry?.mine ?: "") }
    var wh by remember { mutableStateOf(entry?.warehouse ?: "") }
    var weight by remember { mutableStateOf(entry?.weight?.toString() ?: "") }
    val initialTotal = if (entry != null) ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) else 0.0
    var coalAmount by remember { mutableStateOf(if (initialTotal > 0) initialTotal.toLong().toString() else "") }
    var fare by remember { mutableStateOf(entry?.fare?.toLong()?.toString() ?: "") }
    // Advance payment removed per user request
    
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        IOSDatePickerSheet(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryBlue) }
                Text(if (entry == null) "New Entry" else "Edit Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { 
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val total = coalAmount.toDoubleOrNull() ?: 0.0
                    val calculatedRate = if (w > 0) total / w else total
                    onAdd(selectedDate, truck, mine, wh, w, calculatedRate, fare.toDoubleOrNull(), null) 
                }) { 
                    Text("Save", color = PrimaryBlue, fontWeight = FontWeight.Bold) 
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date Selection Row
            Surface(
                onClick = { showDatePicker = true },
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(formatDisplayDate(selectedDate), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumInput(label = "Truck Number", value = truck, onValueChange = { truck = it })
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumInput(label = "Weight (tons)", value = weight, onValueChange = { weight = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    PremiumInput(label = "Coal Amount", value = coalAmount, onValueChange = { coalAmount = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, visualTransformation = ThousandsSeparatorTransformation())
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumInput(label = "Mine Name", value = mine, onValueChange = { mine = it }, modifier = Modifier.weight(1f))
                    PremiumInput(label = "Warehouse", value = wh, onValueChange = { wh = it }, modifier = Modifier.weight(1f))
                }
                
                PremiumInput(label = "Fare / Freight (Rs)", value = fare, onValueChange = { fare = it }, keyboardType = KeyboardType.Number, visualTransformation = ThousandsSeparatorTransformation())
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportActionSheet(
    viewModel: com.example.awancoalledger.viewmodel.LedgerViewModel,
    details: PartyWithDetails,
    balance: Double,
    onDismiss: () -> Unit
) {
    val businessName by viewModel.businessName.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState(initial = "")
    val logoUri by viewModel.companyLogoUri.collectAsState()
    val signatureUri by viewModel.signatureUri.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var fromDate by remember {
        mutableLongStateOf(
            listOfNotNull(
                details.entries.minOfOrNull { it.date },
                details.payments.minOfOrNull { it.date }
            ).minOrNull() ?: (System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
        )
    }
    var toDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    
    val fromState = rememberDatePickerState(initialSelectedDateMillis = fromDate)
    val toState = rememberDatePickerState(initialSelectedDateMillis = toDate)

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromState.selectedDateMillis?.let { fromDate = it }
                    showFromPicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = fromState) }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    toState.selectedDateMillis?.let { toDate = it }
                    showToPicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = toState) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Export Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("FROM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = { showFromPicker = true },
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDisplayDate(fromDate), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("TO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = { showToPicker = true },
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDisplayDate(toDate), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ExportActionRow(Icons.Default.Description, "Download PDF", PrimaryBlue) {
                ExportUtils.generateAndSharePdf(
                    context = context, 
                    details = details, 
                    balance = balance, 
                    startDate = fromDate, 
                    endDate = toDate,
                    businessName = businessName,
                    ownerName = ownerName,
                    ownerPhone = businessPhone,
                    logoUri = logoUri?.toString(),
                    signatureUri = signatureUri?.toString()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ExportActionRow(Icons.Default.TableChart, "Download Excel", SuccessGreen) {
                ExportUtils.generateAndShareExcel(context, details, fromDate, toDate)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExportActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentActionSheet(
    payment: Payment? = null,
    partyType: PartyType,
    onDismiss: () -> Unit,
    onAdd: (Long, Double, PaymentType, String?) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(payment?.date ?: System.currentTimeMillis()) }
    var amount by remember { mutableStateOf(payment?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(payment?.type ?: PaymentType.THEY_PAID) }
    var note by remember { mutableStateOf(payment?.note ?: "") }
    
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        IOSDatePickerSheet(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Close", color = PrimaryBlue) }
                Text(if (payment == null) "Add Payment" else "Edit Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { amount.toDoubleOrNull()?.let { onAdd(selectedDate, it, type, note) } }) { 
                    Text("Done", color = PrimaryBlue, fontWeight = FontWeight.Bold) 
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date Selection Row
            Surface(
                onClick = { showDatePicker = true },
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(formatDisplayDate(selectedDate), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumInput(
                    label = "Amount (Rs)",
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardType = KeyboardType.Number,
                    visualTransformation = ThousandsSeparatorTransformation()
                )
                PremiumInput(
                    label = "Note (Optional)",
                    value = note,
                    onValueChange = { note = it }
                )
            }


            Spacer(modifier = Modifier.height(24.dp))
            Text("TOGGLE TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            val leftLabel = if (partyType == PartyType.BUYER) "He Paid" else "He Paid"
            val rightLabel = if (partyType == PartyType.BUYER) "I Paid" else "I Paid"
            
            Row(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)).padding(2.dp)) {
                SegmentControl(leftLabel, type == PaymentType.THEY_PAID, Modifier.weight(1f)) { type = PaymentType.THEY_PAID }
                SegmentControl(rightLabel, type == PaymentType.I_PAID, Modifier.weight(1f)) { type = PaymentType.I_PAID }
            }
        }
    }
}



@Composable
fun SegmentControl(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = try {
            val number = originalText.toLong()
            DecimalFormat("#,###").format(number)
        } catch (e: Exception) {
            originalText
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformedOffset = 0
                var originalCount = 0
                for (i in formatted.indices) {
                    if (formatted[i] != ',') {
                        originalCount++
                    }
                    transformedOffset++
                    if (originalCount == offset) break
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0
                for (i in 0 until offset.coerceAtMost(formatted.length)) {
                    if (formatted[i] != ',') {
                        originalOffset++
                    }
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}