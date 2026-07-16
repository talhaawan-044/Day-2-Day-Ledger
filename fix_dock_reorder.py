import re

with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

old_dialog = """        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                allTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedItems.contains(tab.route)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (isSelected) {
                                    if (selectedItems.size > 1) {
                                        selectedItems = selectedItems - tab.route
                                    }
                                } else {
                                    if (selectedItems.size < 3) {
                                        selectedItems = selectedItems + tab.route
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            tab.icon, 
                            contentDescription = null, 
                            tint = if (isSelected) com.example.awancoalledger.ui.theme.PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f), 
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            tab.title, 
                            modifier = Modifier.weight(1f), 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        if (isSelected) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Check, 
                                contentDescription = "Selected", 
                                tint = com.example.awancoalledger.ui.theme.PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(20.dp))
                        }
                    }
                    if (index < allTabs.size - 1) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        },"""

new_dialog = """        content = {
            val selectedTabs = selectedItems.mapNotNull { route -> allTabs.find { it.route == route } }
            val unselectedTabs = allTabs.filter { !selectedItems.contains(it.route) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // IN DOCK SECTION
                Text(
                    text = "IN DOCK",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    selectedTabs.forEachIndexed { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Remove button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (selectedItems.size > 1) {
                                        selectedItems = selectedItems - tab.route
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.RemoveCircle,
                                    contentDescription = "Remove",
                                    tint = com.example.awancoalledger.ui.theme.ExpenseRed
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Icon(
                                tab.icon, 
                                contentDescription = null, 
                                tint = com.example.awancoalledger.ui.theme.PrimaryBlue, 
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                tab.title, 
                                modifier = Modifier.weight(1f), 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            // Up/Down Arrows for reordering
                            if (index > 0) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            val newList = selectedItems.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            selectedItems = newList
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            if (index < selectedTabs.size - 1) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            val newList = selectedItems.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            selectedItems = newList
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                        }
                        if (index < selectedTabs.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // MORE SHORTCUTS SECTION
                Text(
                    text = "MORE SHORTCUTS",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    if (unselectedTabs.isEmpty()) {
                        Text(
                            "All shortcuts are in dock",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    } else {
                        unselectedTabs.forEachIndexed { index, tab ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add button
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (selectedItems.size < 3) {
                                            selectedItems = selectedItems + tab.route
                                        }
                                    },
                                    modifier = Modifier.size(24.dp),
                                    enabled = selectedItems.size < 3
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Default.AddCircle,
                                        contentDescription = "Add",
                                        tint = if (selectedItems.size < 3) com.example.awancoalledger.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                androidx.compose.material3.Icon(
                                    tab.icon, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f), 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    tab.title, 
                                    modifier = Modifier.weight(1f), 
                                    color = MaterialTheme.colorScheme.onSurface, 
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                            if (index < unselectedTabs.size - 1) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
                }
            }
        },"""

if old_dialog in content:
    content = content.replace(old_dialog, new_dialog)
    with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'w') as f:
        f.write(content)
    print("Updated DockCustomizationDialog to iOS Edit List style")
else:
    print("Could not find the old dialog block! Let's try regex or substring match.")

