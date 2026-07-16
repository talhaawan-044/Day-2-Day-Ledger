package com.example.awancoalledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.ui.components.MeshGradientBackground
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockScreen(
    onUnlock: (String) -> Boolean,
    ownerName: String = "",
    biometricEnabled: Boolean = false,
    onBiometricTrigger: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    
    val timeStr = remember { mutableStateOf("") }
    val dateStr = remember { mutableStateOf("") }

    // Update time every minute
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while(true) {
            val now = Date()
            timeStr.value = timeFmt.format(now)
            dateStr.value = dateFmt.format(now)
            delay(1000)
        }
    }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(4) {
                shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
            }
            shakeOffset.animateTo(0f)
            delay(300)
            isError = false
            pin = ""
        }
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) {
            delay(500)
            onBiometricTrigger()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        MeshGradientBackground()
        
        // Background Dim/Blur Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .blur(40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // iOS Style Lock Icon & Status
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Time & Date (Large iOS Style)
            Text(
                text = timeStr.value,
                fontSize = 92.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = (-2).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.95f
                }
            )
            Text(
                text = dateStr.value.uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // PIN Dots with error shake
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(x = shakeOffset.value.dp)
            ) {
                Text(
                    text = if (isError) "Incorrect PIN" else "Enter PIN",
                    color = if (isError) ErrorRed else Color.White.copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(4) { index ->
                        PinDot(filled = index < pin.length, isError = isError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Keypad
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "back")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                numbers.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { digit ->
                            when (digit) {
                                "bio" -> {
                                    KeypadButton(
                                        icon = Icons.Default.Fingerprint,
                                        onClick = if (biometricEnabled) onBiometricTrigger else ({}),
                                        isFunctional = true,
                                        enabled = biometricEnabled
                                    )
                                }
                                "back" -> {
                                    KeypadButton(
                                        icon = Icons.Default.Backspace,
                                        onClick = {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        isFunctional = true
                                    )
                                }
                                else -> {
                                    KeypadButton(
                                        text = digit,
                                        onClick = {
                                            if (pin.length < 4) {
                                                pin += digit
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (pin.length == 4) {
                                                    if (!onUnlock(pin)) {
                                                        isError = true
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "Securely logged in as $ownerName",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PinDot(filled: Boolean, isError: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (filled) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val color by animateColorAsState(
        targetValue = when {
            isError -> ErrorRed
            filled -> Color.White
            else -> Color.White.copy(alpha = 0.2f)
        },
        label = "color"
    )

    Box(
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun KeypadButton(
    text: String? = null,
    icon: ImageVector? = null,
    isFunctional: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = if (enabled) onClick else ({}),
        modifier = Modifier.size(76.dp),
        color = if (isFunctional) Color.Transparent else Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
        shape = CircleShape,
        enabled = enabled
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text != null) {
                Text(
                    text = text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.3f)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (enabled) 1f else 0.2f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
