import re

with open('app/src/main/java/com/example/awancoalledger/ui/components/IOSAlertDialog.kt', 'r') as f:
    content = f.read()

# Make the dialog shape more iOS like, remove tonal elevation since iOS doesn't use shadows like material, 
# and use a subtle transparent background for a pseudo-frosted look
content = content.replace(
    """            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            )""",
    """            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 0.dp
            )"""
)

# Update dividers to be more subtle
content = content.replace(
    "HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))",
    "HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), thickness = 0.5.dp)"
)

content = content.replace(
    """        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxHeight()
        )""",
    """        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.5.dp
        )"""
)

# Make the dialog button row slightly taller for iOS feel
content = content.replace(
    """                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),""",
    """                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),"""
)

with open('app/src/main/java/com/example/awancoalledger/ui/components/IOSAlertDialog.kt', 'w') as f:
    f.write(content)
print("Updated IOSAlertDialog.kt")
