package com.example.awancoalledger.ui.theme

import androidx.compose.ui.graphics.Color

// iOS System Colors (Exact shades from screenshots)
val iOSBlue = Color(0xFF007AFF)
val iOSGreen = Color(0xFF56D25B)
val iOSOrange = Color(0xFFFF9500)
val iOSRed = Color(0xFFFF3B30)
val iOSPurple = Color(0xFF5856D6)

// System Backgrounds (Pitch Black for modern OLED look)
val iOSBgDark = Color(0xFF000000)
val iOSSecondaryBgDark = Color(0xFF1C1C1E) // iOS Dark Gray for cards
val iOSBgLight = Color(0xFFF2F2F7) // iOS Background Light
val iOSSecondaryBgLight = Color(0xFFFFFFFF) // iOS Secondary Light

// Theme Aliases
val PrimaryBlue = iOSBlue
val SuccessGreen = iOSGreen
val ErrorRed = iOSRed
val WarningOrange = iOSOrange
val SurfaceColor = iOSSecondaryBgDark
val DarkBg = iOSBgDark
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E93)
val BorderColor = Color(0xFF38383A)
val BorderLight = Color(0xFFC7C7CC)

// Gradients
val CardGradient = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
val InsightGradient = listOf(Color(0xFF1C1C1E), Color(0xFF1C1C1E))