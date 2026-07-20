import sys

def check_braces(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
        
    stack = []
    for i, line in enumerate(lines):
        line = line.split('//')[0] # strip comments
        in_string = False
        for char in line:
            if char == '"':
                in_string = not in_string
            if not in_string:
                if char == '{':
                    stack.append(i + 1)
                elif char == '}':
                    if not stack:
                        print(f"Extra '}}' at line {i + 1}")
                        return
                    stack.pop()
                
    if stack:
        print(f"Missing '}}'. Unclosed '{{' at lines: {stack}")
    else:
        print("Braces match perfectly!")

check_braces('app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt')
