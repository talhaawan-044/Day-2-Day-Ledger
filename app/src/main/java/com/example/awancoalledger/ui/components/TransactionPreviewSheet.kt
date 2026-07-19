package com.example.awancoalledger.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.data.LedgerEntry
import com.example.awancoalledger.data.Payment
import com.example.awancoalledger.data.PaymentType
import com.example.awancoalledger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private fun formatFullDateWithDay(dateMillis: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryPreviewSheet(
    entry: LedgerEntry,
    partyType: com.example.awancoalledger.data.PartyType,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    actionLabel: String = "Edit Entry",
    onShare: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val total = ((entry.weight ?: 0.0) * (entry.rate ?: 0.0)) + (entry.fare ?: 0.0)
    val isBuyer = partyType == com.example.awancoalledger.data.PartyType.BUYER
    val displayColor = if (isBuyer) iOSOrange else ErrorRed
    
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            // Header with Amount and X button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("TRANSACTION DETAILS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Rs. ${String.format(Locale.getDefault(), "%,.0f", total)}", color = displayColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content Card with Icons
            Column(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                IconPreviewRow(Icons.Outlined.CalendarToday, Color(0xFF0A84FF), "Date", formatFullDateWithDay(entry.date))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                
                IconPreviewRow(Icons.Outlined.Description, Color(0xFFBF5AF2), "Type", "COAL")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                
                IconPreviewRow(Icons.Outlined.Place, Color(0xFFFF9F0A), "Mine", entry.mine ?: "-")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                
                IconPreviewRow(Icons.Outlined.LocalShipping, Color(0xFF64D2FF), "Truck Number", entry.truckNumber ?: "-")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                
                IconPreviewRow(Icons.Outlined.Tag, Color(0xFF98989D), "Weight", "${entry.weight ?: 0.0} tons")
                
                if (entry.fare != null && entry.fare > 0.0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                    IconPreviewRow(Icons.Outlined.LocalShipping, Color(0xFF98989D), "Fare / Freight", "Rs. ${String.format(Locale.getDefault(), "%,.0f", entry.fare)}")
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                IconPreviewRow(Icons.Outlined.AttachMoney, Color(0xFF32D74B), "Final Rate", "Rs. ${String.format(Locale.getDefault(), "%,.0f", (entry.weight ?: 0.0) * (entry.rate ?: 0.0))}")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stacked Full-Width Buttons
            PreviewActionRow(
                icon = Icons.Outlined.Share,
                label = "Share via WhatsApp",
                color = SuccessGreen,
                onClick = onShare
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PreviewActionRow(
                icon = Icons.Outlined.Edit,
                label = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                onClick = onAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPreviewSheet(
    payment: Payment,
    partyType: com.example.awancoalledger.data.PartyType,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    actionLabel: String = "Edit Payment",
    onShare: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isIncoming = payment.type == PaymentType.THEY_PAID
    val isBuyer = partyType == com.example.awancoalledger.data.PartyType.BUYER
    
    // Buyer: Received (-) Sent (+)
    // Supplier: Sent (-) Received (+)
    val isNegativeEffect = if (isBuyer) isIncoming else !isIncoming
    val displayColor = if (isNegativeEffect) SuccessGreen else ErrorRed
    val sign = if (isNegativeEffect) "-" else "+"
    
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("TRANSACTION DETAILS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$sign Rs. ${String.format(Locale.getDefault(), "%,.0f", payment.amount)}", color = displayColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                IconPreviewRow(Icons.Outlined.CalendarToday, Color(0xFF0A84FF), "Date", formatFullDateWithDay(payment.date))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                
                IconPreviewRow(Icons.Outlined.AccountBalanceWallet, Color(0xFFBF5AF2), "Type", if (payment.type == PaymentType.THEY_PAID) "PAYMENT RECEIVED" else "PAYMENT SENT")
                
                if (!payment.note.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                    IconPreviewRow(Icons.Outlined.Note, Color(0xFFFF9F0A), "Payment Note", payment.note)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PreviewActionRow(
                icon = Icons.Outlined.Share,
                label = "Share via WhatsApp",
                color = SuccessGreen,
                onClick = onShare
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PreviewActionRow(
                icon = Icons.Outlined.Edit,
                label = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                onClick = onAction
            )
        }
    }
}

@Composable
fun IconPreviewRow(icon: ImageVector, iconTint: Color, label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PreviewActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}
