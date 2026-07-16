import re

with open('app/src/main/java/com/example/awancoalledger/MainActivity.kt', 'r') as f:
    content = f.read()

# We need to find the `FoldersScreen(viewModel, onNavigateToFolder = { ... }, onNavigateToEditor = { ... })` call inside `composable("notes")`
# and append `, onBack = { navController.navigate("summary") { popUpTo(0) } }` to the arguments.

new_content = re.sub(
    r'(FoldersScreen\(\s*viewModel,\s*onNavigateToFolder\s*=\s*\{[\s\S]*?\},\s*onNavigateToEditor\s*=\s*\{[\s\S]*?\}\s*)\)',
    r'\1, onBack = { navController.navigate("summary") { popUpTo(0) } })',
    content
)

with open('app/src/main/java/com/example/awancoalledger/MainActivity.kt', 'w') as f:
    f.write(new_content)

print("Updated MainActivity.kt")
