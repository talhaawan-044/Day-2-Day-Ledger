import os
import re

ui_dir = "app/src/main/java/com/example/awancoalledger/ui/screens"
components_dir = "app/src/main/java/com/example/awancoalledger/ui/components"

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    # Find `modifier = ...` inside List or Grid item contents
    # Actually, it's easier to just find the composables inside `items` and add `Modifier.animateItem()`
    
    # We can try to find SwipeableItem( or StockListCard( etc. inside items
    # and add Modifier.animateItem() to them.
    # But Modifier.animateItem() must be applied to the topmost composable in the item block.
    # In Compose 1.7.0, it's animateItem() on the item's modifier.
    # E.g. SwipeableItem(modifier = Modifier.animateItem(), ...)

    lines = content.split('\n')
    changed = False
    for i in range(len(lines)):
        if "SwipeableItem(" in lines[i] and "Modifier.animateItem()" not in lines[i]:
            # find if it already has a modifier
            # this is complex to do with regex, but we can do a simple replacement if we know the structure
            pass
            
for filename in os.listdir(ui_dir):
    if filename.endswith(".kt"):
        process_file(os.path.join(ui_dir, filename))
