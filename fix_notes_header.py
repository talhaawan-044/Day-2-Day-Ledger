import sys

filepath = "app/src/main/java/com/example/awancoalledger/ui/screens/NotesScreen.kt"

with open(filepath, 'r') as f:
    content = f.read()

target = """                    // ── Navigation row ────────────────────────────────────────────
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
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    NoteSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                                text = {
                                                    Row(
                                                            verticalAlignment =
                                                                    Alignment.CenterVertically
                                                    ) {
                                                        if (sortOrder == order) {
                                                            Icon(
                                                                    Icons.Default.Check,
                                                                    null,
                                                                    tint = PrimaryBlue,
                                                                    modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                        } else {
                                                            Spacer(Modifier.width(24.dp))
                                                        }
                                                        Text(
                                                                order.label,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 15.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    sortOrder = order
                                                    showSortMenu = false
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
                            // Grid / List toggle
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.toggleGridView(!isGridView) 
                            }) {
                                Crossfade(
                                        targetState = isGridView,
                                        animationSpec = tween(200),
                                        label = "viewToggle"
                                ) { grid ->
                                    Icon(
                                            if (grid) Icons.AutoMirrored.Filled.List
                                            else Icons.Default.GridView,
                                            null,
                                            tint = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }

                    // ── Title + note count ────────────────────────────────────────
                    Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                                currentFolder?.name ?: "All Notes",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        AnimatedContent(
                                targetState = filteredNotes.size,
                                transitionSpec = {
                                    slideInVertically { it } togetherWith slideOutVertically { -it }
                                },
                                label = "noteCount"
                        ) { count ->
                            Text(
                                    count.toString(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                                " notes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }"""

replacement = """                    com.example.awancoalledger.ui.components.ScreenHeader(
                        title = currentFolder?.name ?: "All Notes",
                        subtitle = "${filteredNotes.size} notes",
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
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    NoteSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                                text = {
                                                    Row(
                                                            verticalAlignment =
                                                                    Alignment.CenterVertically
                                                    ) {
                                                        if (sortOrder == order) {
                                                            Icon(
                                                                    Icons.Default.Check,
                                                                    null,
                                                                    tint = PrimaryBlue,
                                                                    modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                        } else {
                                                            Spacer(Modifier.width(24.dp))
                                                        }
                                                        Text(
                                                                order.label,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 15.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    sortOrder = order
                                                    showSortMenu = false
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
                            // Grid / List toggle
                            IconButton(onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.toggleGridView(!isGridView) 
                            }) {
                                Crossfade(
                                        targetState = isGridView,
                                        animationSpec = tween(200),
                                        label = "viewToggle"
                                ) { grid ->
                                    Icon(
                                            if (grid) Icons.AutoMirrored.Filled.List
                                            else Icons.Default.GridView,
                                            null,
                                            tint = PrimaryBlue
                                    )
                                }
                            }
                        }
                    )"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Updated NotesScreen.kt successfully")
else:
    print("Target block not found in NotesScreen.kt")
