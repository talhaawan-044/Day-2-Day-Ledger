import re

# 1. Update FoldersScreen.kt
with open('app/src/main/java/com/example/awancoalledger/ui/screens/FoldersScreen.kt', 'r') as f:
    folders_content = f.read()

# Add onBack parameter
folders_content = folders_content.replace(
"""fun FoldersScreen(
    viewModel: LedgerViewModel,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToEditor: (Int?) -> Unit
) {""",
"""fun FoldersScreen(
    viewModel: LedgerViewModel,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToEditor: (Int?) -> Unit,
    onBack: () -> Unit
) {""")

# Replace topBar and bottomBar
old_scaffold = """    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(if (isEditMode) "Done" else "Edit", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Folders",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.EditNote, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    )"""

new_scaffold = """    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                ScreenHeader(
                    title = "Folders",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { showAddFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = PrimaryBlue)
                        }
                        IconButton(onClick = { onNavigateToEditor(null) }) {
                            Icon(Icons.Default.EditNote, contentDescription = "New Note", tint = PrimaryBlue)
                        }
                        TextButton(onClick = { isEditMode = !isEditMode }) {
                            Text(if (isEditMode) "Done" else "Edit", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    )"""

folders_content = folders_content.replace(old_scaffold, new_scaffold)

with open('app/src/main/java/com/example/awancoalledger/ui/screens/FoldersScreen.kt', 'w') as f:
    f.write(folders_content)
print("Updated FoldersScreen.kt")

# 2. Update MainActivity.kt
with open('app/src/main/java/com/example/awancoalledger/MainActivity.kt', 'r') as f:
    main_content = f.read()

old_notes_route = """                composable("notes") {
                    FoldersScreen(viewModel, onNavigateToFolder = { folderId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("folder_notes/$folderId")
                    }, onNavigateToEditor = { noteId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (noteId == null) {
                            navController.navigate("note_editor/-1")
                        } else {
                            navController.navigate("note_editor/$noteId")
                        }
                    })
                }"""

new_notes_route = """                composable("notes") {
                    FoldersScreen(viewModel, onNavigateToFolder = { folderId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("folder_notes/$folderId")
                    }, onNavigateToEditor = { noteId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (noteId == null) {
                            navController.navigate("note_editor/-1")
                        } else {
                            navController.navigate("note_editor/$noteId")
                        }
                    }, onBack = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("summary") {
                            popUpTo(0)
                        }
                    })
                }"""

main_content = main_content.replace(old_notes_route, new_notes_route)

with open('app/src/main/java/com/example/awancoalledger/MainActivity.kt', 'w') as f:
    f.write(main_content)
print("Updated MainActivity.kt")

