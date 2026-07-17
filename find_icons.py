import os
import re

ui_dir = "/mnt/DEVELOPMENT/Day-2-Day-Kotlin-Final/app/src/main/java/com/example/awancoalledger/ui"

for root, dirs, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            if "copy(alpha" in content and "Icon(" in content:
                print(filepath.replace(ui_dir, ""))
