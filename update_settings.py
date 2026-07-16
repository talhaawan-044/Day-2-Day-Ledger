import sys

with open("app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "r") as f:
    lines = f.readlines()

def find_index(substr):
    for i, line in enumerate(lines):
        if substr in line:
            return i
    return -1

cloud_account_idx = find_index('SettingsSection(title = "CLOUD ACCOUNT") {')

before_sections = """            if (currentCategory == null) {
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
                            SettingsRow(icon = tab.icon, title = tab.title, value = "Access " + tab.title, color = PrimaryBlue, isLast = index == unusedModules.size - 1) {
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
                Spacer(modifier = Modifier.height(40.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Surface(onClick = { currentCategory = null }, shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentCategory!!.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                when(currentCategory!!) {
                    SettingsCategory.CLOUD_ACCOUNT -> {
"""

lines.insert(cloud_account_idx, before_sections)

# Recalculate indexes because list changed
internal_tools_start = find_index('SettingsSection(title = "INTERNAL TOOLS") {')
if internal_tools_start != -1:
    brace_count = 0
    internal_tools_end = internal_tools_start
    for i in range(internal_tools_start, len(lines)):
        brace_count += lines[i].count('{')
        brace_count -= lines[i].count('}')
        if brace_count == 0:
            internal_tools_end = i
            break
    del lines[internal_tools_start:internal_tools_end + 1]

business_profile_idx = find_index('SettingsSection(title = "BUSINESS PROFILE") {')
lines.insert(business_profile_idx, "                    }\n                    SettingsCategory.BUSINESS_PROFILE -> {\n")

privacy_idx = find_index('SettingsSection(title = "PRIVACY & SECURITY") {')
lines.insert(privacy_idx, "                    }\n                    SettingsCategory.PRIVACY_SECURITY -> {\n")

pref_idx = find_index('SettingsSection(title = "PREFERENCES") {')
lines.insert(pref_idx, "                    }\n                    SettingsCategory.PREFERENCES -> {\n")
lines.insert(pref_idx + 2, """                SettingsRow(
                        androidx.compose.material.icons.Icons.Default.ViewCarousel,
                        "Customize Dock",
                        "Rearrange bottom tabs",
                        color = SuccessGreen
                ) {
                    showDockCustomizationModal = true
                }
""")

data_idx = find_index('SettingsSection(title = "DATA MANAGEMENT") {')
lines.insert(data_idx, "                    }\n                    SettingsCategory.DATA_MANAGEMENT -> {\n")

spacer_idx = find_index('Spacer(modifier = Modifier.height(100.dp))')
lines.insert(spacer_idx, "                    }\n                }\n            }\n")

with open("app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "w") as f:
    f.writelines(lines)

