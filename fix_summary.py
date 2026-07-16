import sys

filepath = "app/src/main/java/com/example/awancoalledger/ui/screens/SummaryScreen.kt"
with open(filepath, 'r') as f:
    content = f.read()

target = """    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
    ) {
        Column {"""

replacement = """    val haptic = LocalHapticFeedback.current
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    backDispatcher?.onBackPressed()
                },
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Updated SummaryScreen.kt")
else:
    print("Target not found in SummaryScreen.kt")
