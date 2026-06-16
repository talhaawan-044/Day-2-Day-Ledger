package com.example.awancoalledger.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.ui.theme.PrimaryBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF010101))) {
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            val width = size.width
            val height = size.height

            // Deep Blue
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0056D2).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(
                        x = width * (0.3f + 0.4f * cos(phase * 0.5f).toFloat()),
                        y = height * (0.2f + 0.3f * sin(phase * 0.8f).toFloat())
                    ),
                    radius = width * 1.2f
                ),
                radius = width * 1.2f
            )

            // Vibrant Purple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8E5AFF).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(
                        x = width * (0.8f + 0.3f * sin(phase * 0.6f).toFloat()),
                        y = height * (0.7f + 0.4f * cos(phase * 0.4f).toFloat())
                    ),
                    radius = width * 1.0f
                ),
                radius = width * 1.0f
            )

            // Cyan Accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(
                        x = width * (0.5f + 0.5f * cos(phase * 1.2f).toFloat()),
                        y = height * (0.9f + 0.2f * sin(phase * 1.1f).toFloat())
                    ),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f
            )
            
            // Subtle White glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = width * 1.5f
                ),
                radius = width * 1.5f
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    borderAlpha: Float = 0.15f,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        color = Color.White.copy(alpha = 0.07f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Color(0xFF007AFF)
) {
    val gradient = Brush.linearGradient(
        listOf(containerColor, Color(0xFF5856D6))
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                shape = RoundedCornerShape(18.dp)
                clip = true
            },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.White.copy(alpha = 0.1f)
        ),
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) gradient else Brush.linearGradient(listOf(Color.Gray, Color.Gray))),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }
    }
}
