package com.example.awancoalledger.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSDatePickerSheet(
    initialDate: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialDate } }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val years = remember { (2020..2040).toList() }
    val months = remember { 
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        (0..11).map { 
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, it)
            sdf.format(cal.time)
        }
    }
    
    val maxDays = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val days = (1..maxDays).map { it.toString() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                TextButton(onClick = {
                    val result = Calendar.getInstance()
                    result.set(selectedYear, selectedMonth, selectedDay.coerceAtMost(maxDays))
                    onDateSelected(result.timeInMillis)
                }) {
                    Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                )
                Row(modifier = Modifier.fillMaxSize()) {
                    WheelPicker(items = days, initialIndex = (selectedDay - 1).coerceIn(0, days.size - 1), onItemSelected = { selectedDay = it + 1 }, modifier = Modifier.weight(1f))
                    WheelPicker(items = months, initialIndex = selectedMonth, onItemSelected = { selectedMonth = it }, modifier = Modifier.weight(2f))
                    WheelPicker(items = years.map { it.toString() }, initialIndex = years.indexOf(selectedYear).coerceAtLeast(0), onItemSelected = { selectedYear = years[it] }, modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSTimePickerSheet(
    initialTime: Long,
    onDismiss: () -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialTime } }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 40.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                Text("Time", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                TextButton(onClick = {
                    val result = Calendar.getInstance().apply { timeInMillis = initialTime }
                    result.set(Calendar.HOUR_OF_DAY, selectedHour)
                    result.set(Calendar.MINUTE, selectedMinute)
                    onTimeSelected(result.timeInMillis)
                }) {
                    Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                )
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker(items = (0..23).map { it.toString().padStart(2, '0') }, initialIndex = selectedHour, onItemSelected = { selectedHour = it }, modifier = Modifier.weight(1f))
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                    WheelPicker(items = (0..59).map { it.toString().padStart(2, '0') }, initialIndex = selectedMinute, onItemSelected = { selectedMinute = it }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { Math.abs((it.offset + it.size / 2) - center) }?.index ?: 0
            }
        }
    }

    var lastIndex by remember { mutableIntStateOf(initialIndex) }

    LaunchedEffect(centeredIndex) {
        if (centeredIndex != lastIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastIndex = centeredIndex
            onItemSelected(centeredIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size) { index ->
            val itemOffset = remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val itemInfo = visibleItems.find { it.index == index }
                    if (itemInfo != null) {
                        val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                        (itemInfo.offset + itemInfo.size / 2f) - center
                    } else { 1000f }
                }
            }
            val scale = remember { derivedStateOf { (1.15f - (Math.abs(itemOffset.value) / 100f) * 0.3f).coerceAtLeast(0.8f) } }
            val alpha = remember { derivedStateOf { (1f - (Math.abs(itemOffset.value) / 100f) * 0.9f).coerceAtLeast(0.15f) } }
            val rotationX = remember { derivedStateOf { (itemOffset.value / 8f).coerceIn(-40f, 40f) } }

            Box(
                modifier = Modifier.height(40.dp).fillMaxWidth().graphicsLayer {
                    scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value; this.rotationX = rotationX.value
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (index == centeredIndex) 22.sp else 19.sp,
                    fontWeight = if (index == centeredIndex) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (index == centeredIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
