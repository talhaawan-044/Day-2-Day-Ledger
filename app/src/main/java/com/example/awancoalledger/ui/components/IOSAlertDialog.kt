package com.example.awancoalledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.awancoalledger.ui.theme.PrimaryBlue

@Composable
fun IOSAlertDialog(
    title: String,
    message: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable (() -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    if (message != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (content != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            content()
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.IOSDialogButton(
    text: String,
    onClick: () -> Unit,
    color: Color = PrimaryBlue,
    fontWeight: FontWeight = FontWeight.Normal,
    isLast: Boolean = false
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 17.sp,
            fontWeight = fontWeight
        )
    }
    
    if (!isLast) {
        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxHeight()
        )
    }
}
