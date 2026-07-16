import re

with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

old_dialog = """        content = {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 300.dp).padding(top = 8.dp)) {
                items(allTabs.size) { index ->
                    val tab = allTabs[index]
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(tab.icon, contentDescription = null, tint = com.example.awancoalledger.ui.theme.PrimaryBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(tab.title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        if (isSelected) {
                            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Selected", tint = com.example.awancoalledger.ui.theme.SuccessGreen)
                        }
                    }
                }
            }
        },"""

new_dialog = """        content = {
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
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LightImpact)
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

if old_dialog in content:
    content = content.replace(old_dialog, new_dialog)
    with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'w') as f:
        f.write(content)
    print("Updated DockCustomizationDialog in SettingsScreen.kt")
else:
    print("Could not find the old dialog block!")
