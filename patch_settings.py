import re

with open("/mnt/DEVELOPMENT/Day-2-Day-Kotlin-Final/app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add SettingsCategory enum
enum_code = """
import androidx.activity.compose.BackHandler

enum class SettingsCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: androidx.compose.ui.graphics.Color, val subtitle: String) {
    CLOUD_ACCOUNT("Cloud Account", androidx.compose.material.icons.Icons.Default.CloudUpload, PrimaryBlue, "Sync & Backups"),
    BUSINESS_PROFILE("Business Profile", androidx.compose.material.icons.Icons.Default.Business, PrimaryBlue, "Name, phone, logo"),
    PRIVACY_SECURITY("Privacy & Security", androidx.compose.material.icons.Icons.Default.Lock, ErrorRed, "App lock, biometrics"),
    PREFERENCES("Preferences", androidx.compose.material.icons.Icons.Default.SettingsSuggest, iOSPurple, "Dark mode, dock"),
    DATA_MANAGEMENT("Data Management", androidx.compose.material.icons.Icons.Default.History, iOSOrange, "Backups, recovery")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
"""
content = content.replace("@OptIn(ExperimentalMaterial3Api::class)\n@Composable\n", enum_code, 1)

# Add currentCategory state
state_code = """    var showDockCustomizationModal by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var currentCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val dockItems by viewModel.dockItems.collectAsState()

    BackHandler(enabled = currentCategory != null) {
        currentCategory = null
    }"""
content = content.replace("    var showDockCustomizationModal by remember { mutableStateOf(false) }\n    var lastClickTime by remember { mutableLongStateOf(0L) }\n\n    val dockItems by viewModel.dockItems.collectAsState()", state_code)

# Now, we need to restructure the Column inside Box
# The Column starts at line ~178.
# We will replace the sections with a when block.

# Find the start of the Column:
column_start = """        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {"""

new_column_start = """        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (currentCategory == null) {
                Text("Settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                ProfileHeader(ownerName, bizName)
                Spacer(modifier = Modifier.height(28.dp))
                
                val allModules = listOf(
                    com.example.awancoalledger.NavTab("Contacts", "parties", androidx.compose.material.icons.Icons.Default.People),
                    com.example.awancoalledger.NavTab("Expenses", "expenses", androidx.compose.material.icons.Icons.Default.Payments),
                    com.example.awancoalledger.NavTab("Inventory", "inventory", androidx.compose.material.icons.Icons.Default.Layers),
                    com.example.awancoalledger.NavTab("Notes", "notes", androidx.compose.material.icons.Icons.Default.Description),
                    com.example.awancoalledger.NavTab("Vehicles", "vehicle_tracker", androidx.compose.material.icons.Icons.Default.DirectionsCar)
                )
                val unusedModules = allModules.filter { it.route !in dockItems }
                if (unusedModules.isNotEmpty()) {
                    SettingsSection(title = "SHORTCUTS") {
                        unusedModules.forEachIndexed { index, tab ->
                            SettingsRow(icon = tab.icon, title = tab.title, value = "Access ${tab.title}", color = PrimaryBlue, isLast = index == unusedModules.size - 1) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 1000) {
                                    lastClickTime = currentTime
                                    onNavigateToShortcut(tab.route)
                                }
                            }
                        }
                    }
                }
                
                SettingsSection(title = "CATEGORIES") {
                    SettingsCategory.values().forEachIndexed { index, category ->
                        SettingsRow(icon = category.icon, title = category.title, value = category.subtitle, color = category.color, isLast = index == SettingsCategory.values().size - 1) {
                            currentCategory = category
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Surface(onClick = { currentCategory = null }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentCategory!!.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                
                when (currentCategory!!) {
                    SettingsCategory.CLOUD_ACCOUNT -> {"""

content = content.replace(column_start, new_column_start)

# Now we need to wrap the sections
content = content.replace('            Text(\n                    "Settings",', '                /* Text(\n                    "Settings",')
content = content.replace('            Spacer(modifier = Modifier.height(20.dp))\n\n            // Profile Header', '            Spacer(modifier = Modifier.height(20.dp))\n\n            // Profile Header')
content = content.replace('            ProfileHeader(ownerName, bizName)\n\n            Spacer(modifier = Modifier.height(28.dp))', '            ProfileHeader(ownerName, bizName)\n\n            Spacer(modifier = Modifier.height(28.dp)) */')

# Remove the unusedModules block we already moved
unused_block_start = '            val allModules = listOf('
unused_block_end = '                }\n            }\n\n            SettingsSection(title = "BUSINESS PROFILE") {'
content = content.replace(content[content.find(unused_block_start):content.find(unused_block_end)], '')

content = content.replace('            SettingsSection(title = "BUSINESS PROFILE") {', '                    }\n                    SettingsCategory.BUSINESS_PROFILE -> {\n                SettingsSection(title = "BUSINESS PROFILE") {')

content = content.replace('            SettingsSection(title = "PRIVACY & SECURITY") {', '                    }\n                    SettingsCategory.PRIVACY_SECURITY -> {\n                SettingsSection(title = "PRIVACY & SECURITY") {')

content = content.replace('            SettingsSection(title = "PREFERENCES") {', '                    }\n                    SettingsCategory.PREFERENCES -> {\n                SettingsSection(title = "PREFERENCES") {')

content = content.replace('            SettingsSection(title = "DATA MANAGEMENT") {', '                    }\n                    SettingsCategory.DATA_MANAGEMENT -> {\n                SettingsSection(title = "DATA MANAGEMENT") {')

content = content.replace('            Spacer(modifier = Modifier.height(100.dp))\n        }\n\n        // Modals', '                    }\n                }\n                Spacer(modifier = Modifier.height(100.dp))\n            }\n        }\n\n        // Modals')


with open("/mnt/DEVELOPMENT/Day-2-Day-Kotlin-Final/app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

