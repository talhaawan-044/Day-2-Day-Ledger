import os
import re

files = [
    r"e:\STUDIO-LEDGER\AwanCoalLedger\app\src\main\java\com\example\awancoalledger\ui\screens\LedgerDetailScreen.kt",
    r"e:\STUDIO-LEDGER\AwanCoalLedger\app\src\main\java\com\example\awancoalledger\ui\screens\ExpensesScreen.kt",
    r"e:\STUDIO-LEDGER\AwanCoalLedger\app\src\main\java\com\example\awancoalledger\ui\screens\VehicleTrackerScreen.kt"
]

def fix_file(file_path):
    if not os.path.exists(file_path):
        return
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace parameter names
    content = content.replace("state = datePickerState,", "initialDate = selectedDate,")
    content = content.replace("onConfirm = {", "onDateSelected = {")
    
    # Replace logic inside onDateSelected
    content = re.sub(r"datePickerState\.selectedDateMillis\?\.let \{ selectedDate = it \}", "selectedDate = it", content)
    content = re.sub(r"selectedDate = datePickerState\.selectedDateMillis \?: System\.currentTimeMillis\(\)", "selectedDate = it", content)

    # Remove obsolete lines
    lines = content.splitlines()
    new_lines = []
    for line in lines:
        if "val datePickerState = rememberDatePickerState" in line:
            continue
        if "// Hoist the DatePickerState" in line:
            continue
        new_lines.append(line)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(new_lines))

for f in files:
    fix_file(f)
