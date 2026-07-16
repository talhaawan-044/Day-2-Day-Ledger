import sys

with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    new_lines.append(line)
    if 'if (showLoginDialog) {' in line:
        new_lines.append("""        if (showDockCustomizationModal) {
            DockCustomizationDialog(
                currentItems = dockItems,
                onDismiss = { showDockCustomizationModal = false },
                onSave = { selected -> viewModel.updateDockItems(selected) }
            )
        }
""")

with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'w') as f:
    f.writelines(new_lines)

# Also append the Composable
with open('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt', 'a') as f:
    f.write("""
@Composable
fun DockCustomizationDialog(
    currentItems: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val allTabs = listOf(
        com.example.awancoalledger.NavTab("Contacts", "parties", androidx.compose.material.icons.Icons.Default.People),
        com.example.awancoalledger.NavTab("Expenses", "expenses", androidx.compose.material.icons.Icons.Default.Payments),
        com.example.awancoalledger.NavTab("Inventory", "inventory", androidx.compose.material.icons.Icons.Default.Layers),
        com.example.awancoalledger.NavTab("Notes", "notes", androidx.compose.material.icons.Icons.Default.Description),
        com.example.awancoalledger.NavTab("Vehicles", "vehicle_tracker", androidx.compose.material.icons.Icons.Default.DirectionsCar)
    )
    
    var selectedItems by remember { mutableStateOf(currentItems) }

    com.example.awancoalledger.ui.components.IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = "Customize Dock",
        message = "Select exactly 3 shortcuts.",
        content = {
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
        },
        buttons = {
            com.example.awancoalledger.ui.components.IOSDialogButton(
                text = "Cancel",
                onClick = onDismiss,
            )
            com.example.awancoalledger.ui.components.IOSDialogButton(
                text = "Save",
                onClick = {
                    if (selectedItems.size == 3) {
                        onSave(selectedItems)
                        onDismiss()
                    }
                },
                color = if (selectedItems.size == 3) com.example.awancoalledger.ui.theme.PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f),
                fontWeight = FontWeight.Bold,
                isLast = true
            )
        }
    )
}
""")
