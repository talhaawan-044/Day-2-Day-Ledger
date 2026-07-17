package com.example.awancoalledger.ui.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awancoalledger.ui.theme.ErrorRed
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shape

enum class DragAnchors {
    Start, // Edit revealed
    Center, // Default
    End // Delete revealed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableItem(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 84.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val snapAnimSpec = tween<Float>(300)
    val decayAnimSpec = exponentialDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            anchors = DraggableAnchors {
                DragAnchors.Start at actionWidthPx
                DragAnchors.Center at 0f
                DragAnchors.End at -actionWidthPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = snapAnimSpec,
            decayAnimationSpec = decayAnimSpec
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
    ) {
        // Background Actions
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete Action (Left - Revealed by Swipe RIGHT)
            val isDeleteVisible = try { state.offset > 0 } catch(e: Exception) { false }
            SwipeAction(
                label = "Delete",
                icon = Icons.Outlined.Delete,
                backgroundColor = ErrorRed,
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight(),
                isVisible = isDeleteVisible,
                onClick = {
                    onDelete()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Edit Action (Right - Revealed by Swipe LEFT)
            val isEditVisible = try { state.offset < 0 } catch(e: Exception) { false }
            SwipeAction(
                label = "Edit",
                icon = Icons.Outlined.Edit,
                backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight(),
                isVisible = isEditVisible,
                onClick = {
                    onEdit()
                }
            )
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val offset = try { state.offset } catch(e: Exception) { 0f }
                    IntOffset(
                        x = if (offset.isNaN()) 0 else offset.roundToInt(),
                        y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(backgroundColor)
        ) {
            content()
        }
    }
}

@Composable
fun SwipeAction(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onClick: () -> Unit
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
