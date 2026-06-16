package com.example.awancoalledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.awancoalledger.ui.theme.SuccessGreen

@Composable
fun PremiumLineGraph(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = SuccessGreen,
    fillColor: Color = SuccessGreen.copy(alpha = 0.15f),
    showArea: Boolean = true
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (data.size < 2) return@Canvas

        val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val minVal = 0f // Baseline at 0 for volume trend
        val range = maxVal - minVal
        
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minVal) / range * height * animationProgress.value)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prevPoint = points[i - 1]
                val currentPoint = points[i]
                val controlPoint1 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2, prevPoint.y)
                val controlPoint2 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2, currentPoint.y)
                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, currentPoint.x, currentPoint.y)
            }
        }

        if (showArea) {
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = (points.minOfOrNull { it.y } ?: 0f).toFloat(),
                    endY = height
                )
            )
        }

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Add a subtle glow/shadow to the line
        drawPath(
            path = strokePath,
            color = lineColor.copy(alpha = 0.2f),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
