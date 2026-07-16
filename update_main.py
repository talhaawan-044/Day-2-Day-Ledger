import sys

with open("app/src/main/java/com/example/awancoalledger/MainActivity.kt", "r") as f:
    lines = f.readlines()

def find_index(substr):
    for i, line in enumerate(lines):
        if substr in line:
            return i
    return -1

import_idx = 0
for i, line in enumerate(lines):
    if line.startswith("import"):
        import_idx = i

lines.insert(import_idx, "import dev.chrisbanes.haze.*\n")
lines.insert(import_idx, "import dev.chrisbanes.haze.materials.*\n")

# Need to find MainAppContainer and insert hazeState and frostedGlass
# `val frostedGlass by viewModel.isFrostedGlassEnabled.collectAsState()`
# `val hazeState = remember { HazeState() }`
main_container_idx = find_index("fun MainAppContainer(viewModel: LedgerViewModel) {")
if main_container_idx != -1:
    lines.insert(main_container_idx + 3, "    val frostedGlass by viewModel.isFrostedGlassEnabled.collectAsState()\n    val hazeState = remember { HazeState() }\n")

# Modify NavigationBar modifier
nav_bar_idx = find_index("NavigationBar(")
if nav_bar_idx != -1:
    lines.insert(nav_bar_idx + 1, "                    modifier = if (frostedGlass) Modifier.hazeChild(state = hazeState, style = HazeMaterials.thin()) else Modifier,\n")
    # also change containerColor to Transparent when frostedGlass is enabled
    # we need to find containerColor
    container_color_idx = find_index("containerColor = MaterialTheme.colorScheme.surface,")
    if container_color_idx != -1:
        lines[container_color_idx] = "                    containerColor = if (frostedGlass) Color.Transparent else MaterialTheme.colorScheme.surface,\n"

# Modify NavHost modifier to include haze(hazeState)
nav_host_idx = find_index("NavHost(")
if nav_host_idx != -1:
    modifier_idx = find_index("modifier = Modifier")
    if modifier_idx != -1 and modifier_idx > nav_host_idx and modifier_idx < nav_host_idx + 5:
        lines[modifier_idx] = "                modifier = Modifier\n                    .fillMaxSize()\n                    .padding(innerPadding)\n                    .then(if (frostedGlass) Modifier.haze(hazeState) else Modifier),\n"

with open("app/src/main/java/com/example/awancoalledger/MainActivity.kt", "w") as f:
    f.writelines(lines)
