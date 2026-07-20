with open("app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()
content = content.replace("    }\n\n@Composable\nfun ProfileHeader", "    }\n}\n\n@Composable\nfun ProfileHeader")
with open("app/src/main/java/com/example/awancoalledger/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
