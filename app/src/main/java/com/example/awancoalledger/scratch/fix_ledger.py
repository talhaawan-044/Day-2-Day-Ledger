import os

file_path = r"e:\STUDIO-LEDGER\AwanCoalLedger\app\src\main\java\com\example\awancoalledger\ui\screens\LedgerDetailScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace state = datePickerState with initialDate = selectedDate
content = content.replace("state = datePickerState,", "initialDate = selectedDate,")

# Replace onConfirm = { ... datePickerState.selectedDateMillis?.let { selectedDate = it } ... }
# We'll do a simple replace for the pattern
content = content.replace("onConfirm = {", "onDateSelected = {")
content = content.replace("datePickerState.selectedDateMillis?.let { selectedDate = it }", "selectedDate = it")

# Remove the obsolete datePickerState lines
lines = content.splitlines()
new_lines = []
for line in lines:
    if "val datePickerState = rememberDatePickerState" in line:
        continue
    if "// Hoist the DatePickerState" in line:
        continue
    new_lines.append(line)

content = "\n".join(new_lines)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
