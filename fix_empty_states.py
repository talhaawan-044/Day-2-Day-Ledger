import re

def update_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

# 1. PartiesScreen.kt
update_file('app/src/main/java/com/example/awancoalledger/ui/screens/PartiesScreen.kt', [
    (
        """                com.example.awancoalledger.ui.components.EmptyStateCard(
                    icon = Icons.Default.Contacts,
                    title = if (searchQuery.isNotEmpty()) "No Results" else "No Contacts Yet",
                    description = if (searchQuery.isNotEmpty()) "No contacts match \\"$searchQuery\\"." else "Tap the '+' button to add your first contact.",
                    modifier = Modifier.weight(1f)
                )""",
        """                com.example.awancoalledger.ui.components.EmptyStateCard(
                    icon = Icons.Default.Contacts,
                    title = if (searchQuery.isNotEmpty()) "No Results" else "No Contacts Yet",
                    description = if (searchQuery.isNotEmpty()) "No contacts match \\"$searchQuery\\"." else "Add your first contact to start tracking.",
                    actionText = if (searchQuery.isEmpty()) "Add Contact" else null,
                    onAction = if (searchQuery.isEmpty()) { { showAddPartySheet = true } } else null,
                    modifier = Modifier.weight(1f)
                )"""
    )
])

# 2. ExpensesScreen.kt
update_file('app/src/main/java/com/example/awancoalledger/ui/screens/ExpensesScreen.kt', [
    (
        """                    com.example.awancoalledger.ui.components.EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = "No Expenses Found",
                        description = "There are no recorded expenses in this period."
                    )""",
        """                    com.example.awancoalledger.ui.components.EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = "No Expenses Found",
                        description = "There are no recorded expenses in this period.",
                        actionText = "Add Expense",
                        onAction = { showAddDialog = true }
                    )"""
    )
])

# 3. InventoryScreen.kt
update_file('app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt', [
    (
        """                com.example.awancoalledger.ui.components.EmptyStateCard(
                    icon = Icons.Default.Layers,
                    title = "No Inventory Records",
                    description = "There are no inventory entries matching your criteria."
                )""",
        """                com.example.awancoalledger.ui.components.EmptyStateCard(
                    icon = Icons.Default.Layers,
                    title = "No Inventory Records",
                    description = "There are no inventory entries matching your criteria.",
                    actionText = "Add Stock",
                    onAction = { showAddStockDialog = true }
                )"""
    )
])

# 4. VehicleTrackerScreen.kt
update_file('app/src/main/java/com/example/awancoalledger/ui/screens/VehicleTrackerScreen.kt', [
    (
        """                        com.example.awancoalledger.ui.components.EmptyStateCard(
                            icon = Icons.Default.DirectionsCar,
                            title = "No Vehicles Tracked",
                            description = "Tap the + button to add a vehicle."
                        )""",
        """                        com.example.awancoalledger.ui.components.EmptyStateCard(
                            icon = Icons.Default.DirectionsCar,
                            title = "No Vehicles Tracked",
                            description = "Add a vehicle to monitor maintenance logs.",
                            actionText = "Add Vehicle",
                            onAction = { showAddVehicle = true }
                        )"""
    ),
    (
        """                            com.example.awancoalledger.ui.components.EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No Logs Yet",
                                description = "Log your first oil change or maintenance event."
                            )""",
        """                            com.example.awancoalledger.ui.components.EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No Logs Yet",
                                description = "Log your first oil change or maintenance event.",
                                actionText = "Add Log",
                                onAction = { showLogModal = true }
                            )"""
    )
])

# 5. NotesScreen.kt
update_file('app/src/main/java/com/example/awancoalledger/ui/screens/NotesScreen.kt', [
    (
        """                    Text(
                            if (searchQuery.isNotEmpty()) "No notes match \\"$searchQuery\\"."
                            else "Tap the compose button below\\nto create your first note.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                    )
                }""",
        """                    Text(
                            if (searchQuery.isNotEmpty()) "No notes match \\"$searchQuery\\"."
                            else "Create your first note.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = { onAddNote() },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                        ) {
                            Text("Compose Note", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }"""
    )
])

print("Replacements done!")
