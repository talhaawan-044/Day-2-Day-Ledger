import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    count = 0
    for i, line in enumerate(content.split('\n')):
        for char in line:
            if char == '{': count += 1
            elif char == '}': count -= 1
        if count < 0:
            print(f"Too many closing braces at line {i+1}")
            return
            
    if count > 0: print(f"Missing {count} closing braces")
    elif count == 0: print("Braces are perfectly balanced!")

check_braces('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt')
