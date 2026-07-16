import sys

def replace_in_file(filepath, target, replacement):
    with open(filepath, 'r') as f:
        content = f.read()
    if target in content:
        content = content.replace(target, replacement)
        with open(filepath, 'w') as f:
            f.write(content)

# Fix StockGridCard signature
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    "fun StockGridCard(stock: Stock, onClick: () -> Unit) {",
    "fun StockGridCard(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {"
)
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    "modifier = Modifier.fillMaxWidth().height(160.dp),",
    "modifier = modifier.fillMaxWidth().height(160.dp),"
)

# Fix StockListCard signature
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    "fun StockListCard(stock: Stock, onClick: () -> Unit) {",
    "fun StockListCard(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {"
)
# Assuming StockListCard uses a Surface or Box for its root modifier
replace_in_file(
    "app/src/main/java/com/example/awancoalledger/ui/screens/InventoryScreen.kt",
    "Surface(\n            onClick = onClick,\n            modifier = Modifier.fillMaxWidth()",
    "Surface(\n            onClick = onClick,\n            modifier = modifier.fillMaxWidth()"
)

print("Done fixing stock cards")
