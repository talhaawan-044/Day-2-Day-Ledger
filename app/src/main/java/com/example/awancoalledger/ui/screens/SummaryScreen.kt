package com.example.awancoalledger.ui.screens

// ─────────────────────────────────────────────────────────────────────────────
//  HomeScreen.kt  –  Awan Coal Ledger
//  Improved & unified version based on SummaryView.tsx + SummaryScreen.kt
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.LedgerEntry
import com.example.awancoalledger.data.Payment
import com.example.awancoalledger.data.PaymentType
import com.example.awancoalledger.ui.components.EntryPreviewSheet
import com.example.awancoalledger.ui.components.IOSAlertDialog
import com.example.awancoalledger.ui.components.IOSDialogButton
import com.example.awancoalledger.ui.components.PaymentPreviewSheet
import com.example.awancoalledger.ui.components.PremiumLineGraph
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.ui.components.bounceClick
import com.example.awancoalledger.utils.ExportUtils
import com.example.awancoalledger.viewmodel.LedgerViewModel
import com.example.awancoalledger.viewmodel.RecentActivity
import com.example.awancoalledger.viewmodel.SyncStatus
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun timeGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning,"
        in 12..16 -> "Good Afternoon,"
        in 17..20 -> "Good Evening,"
        else -> "Good Night,"
    }
}

private val headerDateFmt = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
private val rowDateFmt = SimpleDateFormat("MMM dd", Locale.getDefault())

// ─── Root Screen ─────────────────────────────────────────────────────────────

@OptIn(
        ExperimentalLayoutApi::class,
        ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
        viewModel: LedgerViewModel,
        onNavigateToLedger: (Int) -> Unit,
        onNavigateToParties: () -> Unit,
        onNavigateToStock: () -> Unit,
        onNavigateToExpenses: () -> Unit,
        onNavigateToNotes: () -> Unit,
        onNavigateToReminders: () -> Unit,
        onNavigateToVehicleTracker: () -> Unit = {}
) {
    // ── State ────────────────────────────────────────────────────────────────
    val recentActivity by viewModel.recentActivity.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val netCredit by viewModel.netMarketCredit.collectAsState()
    val receivable by viewModel.totalReceivable.collectAsState()
    val payable by viewModel.totalPayable.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val totalStockWeight by viewModel.totalInventoryWeight.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val todayExpenses by viewModel.todaysExpenses.collectAsState()
    val partiesCount by viewModel.partiesCount.collectAsState()
    val activeRemindersCount by viewModel.activeRemindersCount.collectAsState()
    val notesCount by viewModel.notesCount.collectAsState()
    val kmsRemaining by viewModel.kmsRemainingPrimary.collectAsState()
    val nextOilChange by viewModel.nextOilChangePrimary.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    var showSyncDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    // Preview State
    var previewingActivity by remember { mutableStateOf<RecentActivity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val onRefresh: () -> Unit = { isRefreshing = true; viewModel.forceSync() }

    LaunchedEffect(syncStatus) {
        if (syncStatus == SyncStatus.Synced || syncStatus == SyncStatus.Error) isRefreshing = false
    }

    val currentDate = remember { headerDateFmt.format(Date()).uppercase() }
    val haptic = LocalHapticFeedback.current

    // ── Layout ───────────────────────────────────────────────────────────────
    PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 112.dp) // clearance for nav + FAB
        ) {
            // 1 ── Header
            HomeHeader(
                    currentDate = currentDate,
                    ownerName = ownerName,
                    syncStatus = syncStatus,
                    onSyncClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSyncDialog = true
                    }
            )

            Spacer(Modifier.height(12.dp))

            // 1b ── Quick Stats
            QuickStatsRow(
                    partiesCount = partiesCount,
                    // remindersCount = activeRemindersCount,
                    notesCount = notesCount,
                    onPartiesClick = onNavigateToParties,
                    // onRemindersClick = onNavigateToReminders,
                    onNotesClick = onNavigateToNotes
            )

            Spacer(Modifier.height(24.dp))

            // 2 ── Net Position Card  (hero card)
            NetPositionCard(
                    netCredit = netCredit,
                    receivable = receivable,
                    payable = payable,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToParties()
                    }
            )

            Spacer(Modifier.height(32.dp))

            // 3 ── Financial Insights Carousel
            FinancialInsightsCarousel(
                    totalIncoming = receivable,
                    monthlyExpenses = monthlyExpenses,
                    todayExpenses = todayExpenses,
                    totalStock = totalStockWeight,
                    onIncomingClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToParties() },
                    onExpensesClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToExpenses() },
                    onStockClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToStock() }
            )

            Spacer(Modifier.height(32.dp))

            // 4 ── Quick Actions Dock
            QuickActionsDock(
                onAddExpense = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToExpenses() },
                onAddParty = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToParties() },
                onInventory = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToStock() },
                onGarage = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToVehicleTracker() }
            )

            Spacer(Modifier.height(32.dp))

            // 4b ── Vehicle Status
            VehicleStatusTile(kmsRemaining, nextOilChange) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigateToVehicleTracker()
            }

            Spacer(Modifier.height(32.dp))

            // 5 ── Latest Transactions
            LatestTransactionsSection(
                    recentActivity = recentActivity,
                    onEntryClick = { activity ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        previewingActivity = activity
                    },
                    onDeleteEntry = { },
                    onEditEntry = { },
                    onSeeAllClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToParties()
                    }
            )
        }


        // ── Sync Detail Dialog ────────────────────────────────────────────────
        if (showSyncDialog) {
            SyncDetailDialog(
                status = syncStatus,
                lastSyncTime = lastSyncTime,
                onDismiss = { showSyncDialog = false },
                onForceSync = { viewModel.forceSync(); showSyncDialog = false }
            )
        }

        // Preview Sheets
        previewingActivity?.let { activity ->
            if (activity.isPayment) {
                activity.payment?.let { payment ->
                    PaymentPreviewSheet(
                        payment = payment,
                        partyType = activity.partyType,
                        onDismiss = { previewingActivity = null },
                        onAction = {
                            previewingActivity = null
                            onNavigateToLedger(payment.partyId)
                        },
                        actionLabel = "Go to Ledger",
                        onShare = { /* Share logic if needed */ }
                    )
                }
            } else {
                activity.entry?.let { entry ->
                    EntryPreviewSheet(
                        entry = entry,
                        partyType = activity.partyType,
                        onDismiss = { previewingActivity = null },
                        onAction = {
                            previewingActivity = null
                            onNavigateToLedger(entry.partyId)
                        },
                        actionLabel = "Go to Ledger",
                        onShare = { ExportUtils.shareDetailedEntry(context, entry) }
                    )
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
fun HomeHeader(
        currentDate: String,
        ownerName: String,
        syncStatus: SyncStatus,
        onSyncClick: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isDaytime = hour in 6..18
    val greetIcon = if (isDaytime) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
    val iconTint = if (isDaytime) iOSOrange else iOSPurple

    val haptic = LocalHapticFeedback.current
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
    ) {
        Column {
            // Date chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                        currentDate,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            // Greeting + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        greetIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                        timeGreeting(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                )
            }
            Text(
                    ownerName.split(" ").firstOrNull() ?: "Boss",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
            )
        }
        SyncBadge(status = syncStatus, onClick = onSyncClick)
    }
}

// ─── Sync Badge ──────────────────────────────────────────────────────────────
// FIX: rotation animation is only applied when status == Syncing

@Composable
fun SyncBadge(status: SyncStatus, onClick: () -> Unit) {
    val (color, label, icon) =
            when (status) {
                SyncStatus.Synced -> Triple(SuccessGreen, "Synced", Icons.Outlined.CloudDone)
                SyncStatus.Syncing -> Triple(PrimaryBlue, "Syncing", Icons.Outlined.Sync)
                SyncStatus.LocalOnly -> Triple(iOSOrange, "Offline", Icons.Outlined.CloudOff)
                SyncStatus.Error -> Triple(ErrorRed, "Error", Icons.Outlined.ErrorOutline)
            }

    // Infinite rotation — but only USED when syncing
    val rotation by
            rememberInfiniteTransition("sync")
                    .animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                            label = "rotation"
                    )

    Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier =
                            Modifier.size(14.dp).graphicsLayer {
                                if (status == SyncStatus.Syncing) rotationZ = rotation
                            }
            )
            Spacer(Modifier.width(6.dp))
            Text(
                    label.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
            )
        }
    }
}

// ─── Net Position Card (hero) ─────────────────────────────────────────────────

@Composable
fun NetPositionCard(netCredit: Double, receivable: Double, payable: Double, onClick: () -> Unit) {
    val isSurplus = netCredit >= 0
    val statusColor = if (isSurplus) SuccessGreen else ErrorRed
    val statusLabel = if (isSurplus) "SURPLUS" else "DEFICIT"
    val statusIcon = if (isSurplus) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown

    // Animate displayed amount on data change
    val animatedNet by
            animateFloatAsState(
                    targetValue = netCredit.toFloat(),
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessVeryLow),
                    label = "netCredit"
            )

    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
    ) {

        Column(modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick).padding(28.dp)) {
            // ── Top row: label + status pill ─────────────────────────────────
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        "NET MARKET CREDIT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                )

                Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                statusIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                                statusLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                        "Rs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                        String.format(
                                Locale.getDefault(),
                                "%,.0f",
                                animatedNet.absoluteValue
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        lineHeight = 48.sp
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Bottom mini stats ─────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStatItem(
                        modifier = Modifier.weight(1f),
                        label = "RECEIVABLE",
                        value = receivable,
                        icon = Icons.Outlined.SouthWest,
                        color = SuccessGreen,
                        textColor = MaterialTheme.colorScheme.onSurface
                )
                Box(
                        Modifier.width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                .align(Alignment.CenterVertically)
                )
                MiniStatItem(
                        modifier = Modifier.weight(1f),
                        label = "PAYABLE",
                        value = payable,
                        icon = Icons.Outlined.NorthEast,
                        color = ErrorRed,
                        textColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MiniStatItem(
        modifier: Modifier,
        label: String,
        value: Double,
        icon: ImageVector,
        color: Color,
        textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                    label,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                "${String.format(Locale.getDefault(), "%.1f", value / 1000)}k",
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MeshGradientBackground() {
    val meshBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6)))
    }
    val radial1 = remember {
        Brush.radialGradient(listOf(Color(0xFF5E5CE6).copy(0.55f), Color.Transparent))
    }
    val radial2 = remember {
        Brush.radialGradient(listOf(Color(0xFF0A84FF).copy(0.35f), Color.Transparent))
    }
    val radial3 = remember {
        Brush.radialGradient(listOf(Color(0xFFFF2D55).copy(0.12f), Color.Transparent))
    }

    androidx.compose.foundation.Canvas(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(220.dp)
    ) {
        drawRect(brush = meshBrush)
        drawCircle(
                brush = radial1,
                radius = 320f,
                center = Offset(size.width * 0.85f, size.height * 0.15f)
        )
        drawCircle(
                brush = radial2,
                radius = 420f,
                center = Offset(size.width * 0.05f, size.height * 1.1f)
        )
        drawCircle(
                brush = radial3,
                radius = 200f,
                center = Offset(size.width * 0.5f, size.height * 0.9f)
        )
    }
}

// ─── Financial Insights Carousel ─────────────────────────────────────────────
// IMPROVED: now has 4 cards  (added "Today's Expenses")

@Composable
fun FinancialInsightsCarousel(
        totalIncoming: Double,
        monthlyExpenses: Double,
        todayExpenses: Double, // NEW
        totalStock: Double,
        onIncomingClick: () -> Unit,
        onExpensesClick: () -> Unit,
        onStockClick: () -> Unit
) {
    Column {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    "Financial Insights",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    "THIS MONTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                InsightCard(
                        title = "Total Incoming",
                        value =
                                "${String.format(Locale.getDefault(), "%.1f", totalIncoming / 1000)}k",
                        subLabel = "Receivable balance",
                        icon = Icons.Outlined.ArrowDownward,
                        color = SuccessGreen,
                        onClick = onIncomingClick
                )
            }
            item {
                InsightCard(
                        title = "OpEx (Monthly)",
                        value =
                                "${String.format(Locale.getDefault(), "%.1f", monthlyExpenses / 1000)}k",
                        subLabel = "This month's spend",
                        icon = Icons.Outlined.ShoppingBag,
                        color = iOSPurple,
                        onClick = onExpensesClick
                )
            }
            item {
                InsightCard(
                        title = "Today's Expenses",
                        value =
                                "${String.format(Locale.getDefault(), "%.1f", todayExpenses / 1000)}k",
                        subLabel = "Spent today",
                        icon = Icons.Outlined.ReceiptLong,
                        color = iOSOrange,
                        onClick = onExpensesClick
                )
            }
            item {
                InsightCard(
                        title = "Stock on Hand",
                        value = "${totalStock.toInt()} tons",
                        subLabel = "Current inventory",
                        icon = Icons.Outlined.LocalShipping,
                        color = PrimaryBlue,
                        onClick = onStockClick
                )
            }
        }
    }
}

@Composable
fun InsightCard(
        title: String,
        value: String,
        subLabel: String,
        icon: ImageVector,
        color: Color,
        onClick: () -> Unit
) {
    Surface(
            onClick = onClick,
            modifier = Modifier.size(width = 168.dp, height = 148.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(color = color, shape = RoundedCornerShape(10.dp)) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Column {
                Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                        value,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                )
                Text(
                        subLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// ─── Latest Transactions Section ─────────────────────────────────────────────

@Composable
fun LatestTransactionsSection(
        recentActivity: List<RecentActivity>,
        onEntryClick: (RecentActivity) -> Unit,
        onDeleteEntry: (RecentActivity) -> Unit,
        onEditEntry: (RecentActivity) -> Unit,
        onSeeAllClick: () -> Unit
) {
    Column {
        // Section header
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    "Latest Transactions",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("See All", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBlue
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column {
                if (recentActivity.isEmpty()) {
                    // ── Empty state ────────────────────────────────────────────
                    Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                                Icons.Outlined.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                                "No recent activity",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                                "Transactions will appear here",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 13.sp
                        )
                    }
                } else {
                    recentActivity.forEachIndexed { index, activity ->
                        ActivityRow(activity = activity, onClick = { onEntryClick(activity) })
                        if (index < recentActivity.lastIndex) {
                            HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp),
                                    color =
                                            MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.4f
                                            ),
                                    thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Activity Row ─────────────────────────────────────────────────────────────
// IMPROVED: distinguishes Stock Arrival  vs Coal Dispatch vs Payment

@Composable
fun ActivityRow(activity: RecentActivity, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val entry = activity.entry

    // Determine row type for icon + color + subtitle
    val isPayment = activity.isPayment
    val isStockArrival = !isPayment && entry?.truckNumber == "STOCK"

    val (rowIcon, rowColor) =
            when {
                isPayment -> Icons.Outlined.AccountBalanceWallet to SuccessGreen
                isStockArrival -> Icons.Outlined.Inventory2 to iOSOrange
                else -> Icons.Outlined.LocalShipping to PrimaryBlue
            }

    val subtitle =
            when {
                isPayment -> "Payment Received • ${rowDateFmt.format(Date(activity.date))}"
                isStockArrival -> "Stock Arrival • ${rowDateFmt.format(Date(activity.date))}"
                else ->
                        "${entry?.truckNumber ?: "Truck"} • ${rowDateFmt.format(Date(activity.date))}"
            }

    val amountText = when {
        isPayment -> "Rs. ${String.format(Locale.getDefault(), "%,.0f", activity.amount)}"
        activity.amount > 0 -> "Rs. ${String.format(Locale.getDefault(), "%,.0f", activity.amount)}"
        else -> "${String.format(Locale.getDefault(), "%.1f", entry?.weight ?: 0.0)} t"
    }
    val amountColor = when {
        isPayment && activity.payment?.type == PaymentType.THEY_PAID -> SuccessGreen
        isPayment -> ErrorRed
        activity.amount > 0 -> MaterialTheme.colorScheme.onSurface
        else -> PrimaryBlue
    }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon badge
        Surface(
                modifier = Modifier.size(48.dp),
                color = rowColor,
                shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                        rowIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Party name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    activity.partyName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Amount / weight
        Text(
                amountText,
                color = amountColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = (-0.3).sp
        )
    }
}

// ─── Quick Stats Row ──────────────────────────────────────────────────────────

@Composable
fun QuickStatsRow(
    partiesCount: Int,
    // remindersCount: Int,
    notesCount: Int,
    onPartiesClick: () -> Unit,
    // onRemindersClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatChip(Modifier.weight(1f), label = "Parties", value = partiesCount, icon = Icons.Outlined.People, color = PrimaryBlue, onClick = onPartiesClick)
        // QuickStatChip(Modifier.weight(1f), label = "Reminders", value = remindersCount, icon = Icons.Outlined.NotificationsActive, color = iOSOrange, onClick = onRemindersClick)
        QuickStatChip(Modifier.weight(1f), label = "Notes", value = notesCount, icon = Icons.Outlined.StickyNote2, color = iOSPurple, onClick = onNotesClick)
    }
}

@Composable
fun QuickStatChip(modifier: Modifier, label: String, value: Int, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(116.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp), 
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(bottom = 2.dp)) {
                Text("$value", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── Mini Action FAB ─────────────────────────────────────────────────────────

@Composable
fun MiniActionFab(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp
        ) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) }
    }
}

// ─── Sync Detail Dialog ───────────────────────────────────────────────────────

@Composable
fun SyncDetailDialog(status: SyncStatus, lastSyncTime: Long?, onDismiss: () -> Unit, onForceSync: () -> Unit) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault()) }
    val (color, statusText) = when (status) {
        SyncStatus.Synced -> SuccessGreen to "All data synced"
        SyncStatus.Syncing -> PrimaryBlue to "Syncing in progress..."
        SyncStatus.LocalOnly -> iOSOrange to "Offline – not signed in"
        SyncStatus.Error -> ErrorRed to "Sync failed"
    }
    IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = "Cloud Sync",
        message = statusText,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                if (lastSyncTime != null) {
                    Text("Last synced: ${timeFmt.format(Date(lastSyncTime))}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    Text("Not synced yet this session", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                if (status == SyncStatus.LocalOnly) {
                    Text("Sign in to enable cloud backup and sync across devices.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        },
        buttons = {
            IOSDialogButton(
                text = "Close",
                onClick = onDismiss
            )
            IOSDialogButton(
                text = "Sync Now",
                fontWeight = FontWeight.Bold,
                isLast = true,
                onClick = onForceSync
            )
        }
    )
}

@Composable
fun VehicleStatusTile(kmsLeft: Double, nextMileage: Double, onClick: () -> Unit) {
    val isOverdue = kmsLeft <= 0 && nextMileage > 0
    val isWarning = kmsLeft < 500 && nextMileage > 0
    
    val bgColor = when {
        isOverdue -> ErrorRed
        isWarning -> iOSOrange
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isOverdue || isWarning) Color.White else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isOverdue || isWarning) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isOverdue || isWarning) Color.White.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DirectionsCar, 
                    null, 
                    tint = if (isOverdue || isWarning) Color.White else PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOverdue) "OIL CHANGE OVERDUE" else if (isWarning) "OIL CHANGE SOON" else "Vehicle Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = subTextColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when {
                        nextMileage <= 0 -> "No data tracked"
                        isOverdue -> "${(-kmsLeft).toInt()} km overdue!"
                        else -> "${kmsLeft.toInt()} km remaining"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = subTextColor)
        }
    }
}

@Composable
fun QuickActionsDock(
    onAddExpense: () -> Unit,
    onAddParty: () -> Unit,
    onInventory: () -> Unit,
    onGarage: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "QUICK ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton("Expense", Icons.Outlined.Payments, iOSOrange, onAddExpense)
                QuickActionButton("Contact", Icons.Outlined.PersonAdd, PrimaryBlue, onAddParty)
                QuickActionButton("Inventory", Icons.Outlined.Inventory, iOSPurple, onInventory)
                QuickActionButton("Garage", Icons.Outlined.DirectionsCar, SuccessGreen, onGarage)
            }
        }
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick { onClick() }) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = color,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
