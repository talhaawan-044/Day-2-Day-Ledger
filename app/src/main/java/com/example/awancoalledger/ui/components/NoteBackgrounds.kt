package com.example.awancoalledger.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.awancoalledger.R

object NoteBackgrounds {
    const val NONE = 0
    const val PARCHMENT = 1
    const val LEATHER_DARK = 2
    const val CRUMPLED_PAPER = 3
    const val LINED_PAPER = 4
    const val GRAPH_PAPER = 5
    const val DOT_GRID = 6

    val allBackgrounds = listOf(
        NONE, PARCHMENT, LEATHER_DARK, CRUMPLED_PAPER, LINED_PAPER, GRAPH_PAPER, DOT_GRID
    )

    fun getName(id: Int): String = when(id) {
        NONE -> "Solid Color"
        PARCHMENT -> "Vintage Parchment"
        LEATHER_DARK -> "Dark Leather"
        CRUMPLED_PAPER -> "Crumpled Paper"
        LINED_PAPER -> "Lined Paper"
        GRAPH_PAPER -> "Grid Paper"
        DOT_GRID -> "Dot Grid"
        else -> "Solid Color"
    }
}

@Composable
fun NoteBackgroundRenderer(bgImageId: Int, contentColor: Color, modifier: Modifier = Modifier) {
    when (bgImageId) {
        NoteBackgrounds.PARCHMENT -> {
            Image(
                painter = painterResource(id = R.drawable.bg_parchment),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
                alpha = 0.85f
            )
        }
        NoteBackgrounds.LEATHER_DARK -> {
            Image(
                painter = painterResource(id = R.drawable.bg_leather_dark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        }
        NoteBackgrounds.CRUMPLED_PAPER -> {
            Image(
                painter = painterResource(id = R.drawable.bg_crumpled_paper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
                alpha = 0.5f // blend with background color
            )
        }
        NoteBackgrounds.LINED_PAPER -> {
            val lineColor = contentColor.copy(alpha = 0.15f)
            Canvas(modifier = modifier.fillMaxSize()) {
                val lineSpacing = 80f
                var y = lineSpacing
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                    y += lineSpacing
                }
                // Left margin line
                drawLine(
                    color = Color(0xFFE57373).copy(alpha = 0.5f),
                    start = Offset(100f, 0f),
                    end = Offset(100f, size.height),
                    strokeWidth = 3f
                )
            }
        }
        NoteBackgrounds.GRAPH_PAPER -> {
            val lineColor = contentColor.copy(alpha = 0.12f)
            Canvas(modifier = modifier.fillMaxSize()) {
                val spacing = 60f
                var y = spacing
                while (y < size.height) {
                    drawLine(color = lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.5f)
                    y += spacing
                }
                var x = spacing
                while (x < size.width) {
                    drawLine(color = lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.5f)
                    x += spacing
                }
            }
        }
        NoteBackgrounds.DOT_GRID -> {
            val dotColor = contentColor.copy(alpha = 0.2f)
            Canvas(modifier = modifier.fillMaxSize()) {
                val spacing = 50f
                var y = spacing
                while (y < size.height) {
                    var x = spacing
                    while (x < size.width) {
                        drawCircle(color = dotColor, radius = 2.5f, center = Offset(x, y))
                        x += spacing
                    }
                    y += spacing
                }
            }
        }
    }
}
