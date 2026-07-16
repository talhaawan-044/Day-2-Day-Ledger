import java.io.File

val file = File("app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt")
var lines = file.readLines().toMutableList()

// 1. Add unused modules logic and Categories menu before CLOUD ACCOUNT
val cloudAccountIndex = lines.indexOfFirst { it.contains("SettingsSection(title = \"CLOUD ACCOUNT\") {") }

val beforeSections = """
            if (currentCategory == null) {
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
""".trimIndent().lines()

lines.addAll(cloudAccountIndex, beforeSections)

// Now we need to remove the "INTERNAL TOOLS" block
// Find INTERNAL TOOLS
var internalToolsStart = lines.indexOfFirst { it.contains("SettingsSection(title = \"INTERNAL TOOLS\") {") }
var internalToolsEnd = internalToolsStart
var braceCount = 0
for (i in internalToolsStart until lines.size) {
    if (lines[i].contains("{")) braceCount += lines[i].count { it == '{' }
    if (lines[i].contains("}")) braceCount -= lines[i].count { it == '}' }
    if (braceCount == 0) {
        internalToolsEnd = i
        break
    }
}
lines.subList(internalToolsStart, internalToolsEnd + 1).clear()

// Update "BUSINESS PROFILE" to be a new branch
val businessProfileIndex = lines.indexOfFirst { it.contains("SettingsSection(title = \"BUSINESS PROFILE\") {") }
lines.add(businessProfileIndex, "                    }")
lines.add(businessProfileIndex + 1, "                    SettingsCategory.BUSINESS_PROFILE -> {")

// Update "PRIVACY & SECURITY"
val privacyIndex = lines.indexOfFirst { it.contains("SettingsSection(title = \"PRIVACY & SECURITY\") {") }
lines.add(privacyIndex, "                    }")
lines.add(privacyIndex + 1, "                    SettingsCategory.PRIVACY_SECURITY -> {")

// Update "PREFERENCES" and add DockCustomization button inside it
val prefIndex = lines.indexOfFirst { it.contains("SettingsSection(title = \"PREFERENCES\") {") }
lines.add(prefIndex, "                    }")
lines.add(prefIndex + 1, "                    SettingsCategory.PREFERENCES -> {")
// Add the dock button just inside the PREFERENCES SettingsSection
lines.add(prefIndex + 3, """
                SettingsRow(
                        androidx.compose.material.icons.Icons.Default.ViewCarousel,
                        "Customize Dock",
                        "Rearrange bottom tabs",
                        color = SuccessGreen
                ) {
                    showDockCustomizationModal = true
                }
""".trimIndent())

// Update "DATA MANAGEMENT"
val dataIndex = lines.indexOfFirst { it.contains("SettingsSection(title = \"DATA MANAGEMENT\") {") }
lines.add(dataIndex, "                    }")
lines.add(dataIndex + 1, "                    SettingsCategory.DATA_MANAGEMENT -> {")

// Close the when and else block before Spacer(modifier = Modifier.height(100.dp))
val spacerIndex = lines.indexOfFirst { it.contains("Spacer(modifier = Modifier.height(100.dp))") }
lines.add(spacerIndex, "                    }")
lines.add(spacerIndex + 1, "                }")
lines.add(spacerIndex + 2, "            }")

file.writeText(lines.joinToString("\n"))

