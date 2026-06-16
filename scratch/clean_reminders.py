import os

file_path = r"e:\STUDIO-LEDGER\AwanCoalLedger\app\src\main\java\com\example\awancoalledger\ui\screens\RemindersScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "fun IOSDatePickerSheet" in line:
        skip = True
    if "fun IOSTimePickerSheet" in line:
        skip = True
    if "fun WheelPicker" in line:
        skip = True
    
    if not skip:
        # Add import if not present
        if "package com.example.awancoalledger.ui.screens" in line:
            new_lines.append(line)
            new_lines.append("import com.example.awancoalledger.ui.components.IOSDatePickerSheet\n")
            new_lines.append("import com.example.awancoalledger.ui.components.IOSTimePickerSheet\n")
            continue
        new_lines.append(line)
    
    if skip and line.strip() == "}":
        # Check if next line is also a closing brace or empty to avoid over-skipping
        # but in this file the functions end with a closing brace on a new line.
        skip = False
        continue

# The logic above is a bit risky if there are nested braces. 
# Better: remove exactly the functions by name and their blocks.

# Let's try again with a simpler approach: remove everything from fun IOSDatePickerSheet to the end of the file (since they are at the end)
content = "".join(lines)
idx = content.find("fun IOSDatePickerSheet")
if idx != -1:
    content = content[:idx]

# Add imports
if "import com.example.awancoalledger.ui.components.IOSDatePickerSheet" not in content:
    content = content.replace("package com.example.awancoalledger.ui.screens", 
                              "package com.example.awancoalledger.ui.screens\n\nimport com.example.awancoalledger.ui.components.IOSDatePickerSheet\nimport com.example.awancoalledger.ui.components.IOSTimePickerSheet")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
