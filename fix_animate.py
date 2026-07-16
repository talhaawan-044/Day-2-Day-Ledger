import sys

def replace_in_file(filepath, target_line, new_line):
    with open(filepath, 'r') as f:
        content = f.read()
    if target_line in content:
        content = content.replace(target_line, new_line)
        with open(filepath, 'w') as f:
            f.write(content)

# ExpensesScreen.kt
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/ExpensesScreen.kt",
    """                        SwipeableItem(
                                onEdit = {""",
    """                        SwipeableItem(
                                modifier = Modifier.animateItem(),
                                onEdit = {"""
)

# InventoryScreen.kt
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    """                        StockListCard(
                                stock,""",
    """                        StockListCard(
                                stock,
                                modifier = Modifier.animateItem(),"""
)
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    """                        StockGridCard(
                                stock,""",
    """                        StockGridCard(
                                stock,
                                modifier = Modifier.animateItem(),"""
)

# PartiesScreen.kt
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/PartiesScreen.kt",
    """                                SwipeableItem(
                                        onEdit = {""",
    """                                SwipeableItem(
                                        modifier = Modifier.animateItem(),
                                        onEdit = {"""
)

# LedgerDetailScreen.kt
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/LedgerDetailScreen.kt",
    """                        SwipeableItem(
                                onEdit = {""",
    """                        SwipeableItem(
                                modifier = Modifier.animateItem(),
                                onEdit = {"""
)

print("Done script")
