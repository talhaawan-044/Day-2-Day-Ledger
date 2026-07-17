package com.example.awancoalledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.ui.theme.*
import kotlinx.coroutines.delay

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
    
    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Sophisticated decaying shake animation (iOS style)
            shakeOffset.animateTo(-30f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(30f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(-20f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(20f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(-10f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(10f, tween(50, easing = LinearEasing))
            shakeOffset.animateTo(0f, tween(50, easing = LinearOutSlowInEasing))
            
            delay(150)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isError) "Incorrect Passcode" else "Enter Passcode",
                color = if (isError) ErrorRed else MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.offset(x = shakeOffset.value.dp)
            ) {
                repeat(4) { index ->
                    PinDot(filled = index < pin.length, isError = isError)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Keypad
            val numbers = listOf(
                listOf("1" to "", "2" to "A B C", "3" to "D E F"),
                listOf("4" to "G H I", "5" to "J K L", "6" to "M N O"),
                listOf("7" to "P Q R S", "8" to "T U V", "9" to "W X Y Z"),
                listOf("bio" to "", "0" to "", "back" to "")
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
                        row.forEach { (digit, letters) ->
                            when (digit) {
                                "bio" -> {
                                    KeypadButton(
                                        icon = Icons.Outlined.Fingerprint,
                                        onClick = if (biometricEnabled) onBiometricTrigger else ({}),
                                        isFunctional = true,
                                        enabled = biometricEnabled
                                    )
                                }
                                "back" -> {
                                    KeypadButton(
                                        icon = Icons.Outlined.Backspace,
                                        onClick = {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        isFunctional = true,
                                        enabled = pin.isNotEmpty()
                                    )
                                }
                                else -> {
                                    KeypadButton(
                                        text = digit,
                                        subText = letters.takeIf { it.isNotEmpty() },
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
            
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun PinDot(filled: Boolean, isError: Boolean) {
    val borderColor = if (isError) ErrorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    val fillColor = if (isError) ErrorRed else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .background(if (filled) fillColor else Color.Transparent)
    )
}

@Composable
fun KeypadButton(
    text: String? = null,
    subText: String? = null,
    icon: ImageVector? = null,
    isFunctional: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (isFunctional) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    val contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.3f)

    Surface(
        onClick = if (enabled) onClick else ({}),
        modifier = Modifier.size(76.dp),
        color = bgColor,
        shape = CircleShape,
        enabled = enabled
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light,
                        color = contentColor,
                        modifier = Modifier.offset(y = if (subText != null) 4.dp else 0.dp)
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            letterSpacing = 1.sp,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
