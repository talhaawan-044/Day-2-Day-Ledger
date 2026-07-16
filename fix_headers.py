import sys

def replace_in_file(filepath, target, replacement):
    with open(filepath, 'r') as f:
        content = f.read()
    if target in content:
        content = content.replace(target, replacement)
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")
    else:
        print(f"Target not found in {filepath}")

# ExpensesScreen
expenses_target = """            // iOS Style Large Header
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        "Expenses",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                )

                // Export Button (Triggers Bottom Sheet)
                IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExportSheet = true
                        },
                        modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = "Export", tint = PrimaryBlue)
                }
            }"""

expenses_replace = """            val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            com.example.awancoalledger.ui.components.ScreenHeader(
                title = "Expenses",
                onBack = { backDispatcher?.onBackPressed() },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExportSheet = true
                        },
                        modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export", tint = PrimaryBlue)
                    }
                }
            )"""

replace_in_file("app/src/main/java/com/example/awancoalledger/ui/screens/ExpensesScreen.kt", expenses_target, expenses_replace)

# NotesScreen
notes_target = """                    // ── Navigation row ────────────────────────────────────────────
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onBack() 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryBlue)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Sort
                            Box {
                                IconButton(onClick = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showSortMenu = true 
                                }) {
                                    Icon(Icons.Default.Sort, null, tint = PrimaryBlue)
                                }
                                DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                ) {
                                    NoteSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                                text = { Text(order.displayName) },
                                                onClick = {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    sortOrder = order
                                                    showSortMenu = false
                                                },
                                                trailingIcon = {
                                                    if (sortOrder == order) {
                                                        Icon(
                                                                Icons.Default.Check,
                                                                null,
                                                                tint = PrimaryBlue
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                            // Add Folder
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showAddFolderDialog = true 
                            }) {
                                Icon(Icons.Default.CreateNewFolder, null, tint = PrimaryBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            "Notes",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                    )"""

notes_replace = """                    com.example.awancoalledger.ui.components.ScreenHeader(
                        title = "Notes",
                        onBack = { onBack() },
                        actions = {
                            Box {
                                IconButton(onClick = { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showSortMenu = true 
                                }) {
                                    Icon(Icons.Default.Sort, null, tint = PrimaryBlue)
                                }
                                DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                ) {
                                    NoteSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                                text = { Text(order.displayName) },
                                                onClick = {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    sortOrder = order
                                                    showSortMenu = false
                                                },
                                                trailingIcon = {
                                                    if (sortOrder == order) {
                                                        Icon(
                                                                Icons.Default.Check,
                                                                null,
                                                                tint = PrimaryBlue
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                            // Add Folder
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showAddFolderDialog = true 
                            }) {
                                Icon(Icons.Default.CreateNewFolder, null, tint = PrimaryBlue)
                            }
                        }
                    )"""

replace_in_file("app/src/main/java/com/example/awancoalledger/ui/screens/NotesScreen.kt", notes_target, notes_replace)

# InventoryScreen
inventory_target = """                Text(
                        "Stock",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )"""

inventory_replace = """                val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                com.example.awancoalledger.ui.components.ScreenHeader(
                    title = "Stock",
                    onBack = { backDispatcher?.onBackPressed() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )"""

replace_in_file("app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt", inventory_target, inventory_replace)

# PartiesScreen
parties_target = """            Text(
                    "Contacts",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
            )"""

parties_replace = """            val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            com.example.awancoalledger.ui.components.ScreenHeader(
                title = "Contacts",
                onBack = { backDispatcher?.onBackPressed() },
                modifier = Modifier.padding(horizontal = 0.dp) // already inside padded Column
            )"""

replace_in_file("app/src/main/java/com/example/awancoalledger/ui/screens/PartiesScreen.kt", parties_target, parties_replace)

# VehicleTrackerScreen
vehicle_target = """            // --- iOS Premium Header ---
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = PrimaryBlue
                        )
                    }
                    Text("Garage", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = { showAddVehicle = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = PrimaryBlue
                        )
                    }
                }
            }"""

vehicle_replace = """            // --- iOS Premium Header ---
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                com.example.awancoalledger.ui.components.ScreenHeader(
                    title = "Garage",
                    onBack = { onNavigateBack() },
                    actions = {
                        IconButton(onClick = { showAddVehicle = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }
                )
            }"""

replace_in_file("app/src/main/java/com/example/awancoalledger/ui/screens/VehicleTrackerScreen.kt", vehicle_target, vehicle_replace)

