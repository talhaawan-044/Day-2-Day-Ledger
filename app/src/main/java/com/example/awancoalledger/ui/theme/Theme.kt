package com.example.awancoalledger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
        lightColorScheme(
                primary = PrimaryBlue,
                secondary = WarningOrange,
                tertiary = SuccessGreen,
                background = Color(0xFFF2F2F7), // iOS Background Light
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black,
                outline = Color(0xFFC7C7CC),
                surfaceVariant = Color(0xFFE5E5EA),
                onSurfaceVariant = Color.Gray
        )

private val DarkColorScheme =
        darkColorScheme(
                primary = PrimaryBlue,
                secondary = WarningOrange,
                tertiary = SuccessGreen,
                background = DarkBg,
                surface = SurfaceColor,
                onPrimary = TextPrimary,
                onSecondary = TextPrimary,
                onTertiary = TextPrimary,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                outline = BorderColor,
                surfaceVariant = Color(0xFF1C1C1E),
                onSurfaceVariant = TextSecondary
        )

@Composable
fun AwanCoalLedgerTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        accentColorHex: String = "#007AFF",
        content: @Composable () -> Unit
) {
    val primaryColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { PrimaryBlue }
    val colorScheme = if (darkTheme) DarkColorScheme.copy(primary = primaryColor) else LightColorScheme.copy(primary = primaryColor)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
