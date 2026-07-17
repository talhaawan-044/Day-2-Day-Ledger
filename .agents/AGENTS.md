# Project Rules & Context

## Branching & Environment Isolation Strategy

**Current Status**: We are actively developing major experimental features on the `feature/big-updates` branch. 

### Critical Rules for AI Agents:

1. **DO NOT Push to `main`:** 
   Under no circumstances should code from `feature/big-updates` be automatically pushed or merged to the `main` branch without explicit, multi-step confirmation from the user. `main` contains the stable production app.

2. **DO NOT Enable Live Sync for Debug:**
   The `debug` build type has been configured with `applicationIdSuffix = ".dev"` to isolate it from the live app database on the user's phone.
   - The `.dev` app ID is included in `google-services.json` *solely* to allow the Gradle build to pass.
   - **CRITICAL:** Do NOT attempt to "fix" Firebase sync for the `.dev` app by creating a new project or linking it to the live database unless the user explicitly requests "Option B" (Live Sync for Dev). The Dev app is intentionally meant to operate locally/offline to prevent polluting the live Firebase database.

3. **Development App Identity:**
   - Package: `com.example.awancoalledger.dev`
   - App Name: "Coal Ledger (Dev)" (defined in `src/debug/res/values/strings.xml`)
   - It will install alongside the live app and generate a fresh, empty Room database. Do not attempt to migrate live data into it.

4. **UI Updates & Screenshots Workflow:**
   - The user will drop UI screenshots via KDE connect into `/mnt/DEVELOPMENT/Day-2-Day-Kotlin-Final/App-screenshots-for-ai/` for AI agents to review. Whenever User mentions screenshots, check this folder.
   - **CRITICAL:** Always check this directory when the user mentions screenshots. Once the screenshot has been analyzed and the corresponding task is complete, the AI **MUST** delete the screenshot to keep the directory clean.

5. **Some UI Instructions:**
   - Use iOS Design system.
   - Use Curvy style for buttons and other UI elements.
   - Don't use glowy, gradient buttons with shadows.
   - No where in the ui, use glow unless it's the ui element which is under the frosted glass.
   - This app is ios inspired so we are not gonna use any emojis or generic ai generate icons
     but ios style vector icons.

6. **Real-Life Data & Backward Compatibility (CRITICAL):**
   - The main app is actively used in real life by the user. The data stored in Firebase Firestore and the local Room database is live, critical data.
   - When introducing changes in the `feature/big-updates` dev branch, **NEVER** write code, schemas, or migrations that could nuke, reset, or corrupt the data currently present in the production schema when we push these updates and changes which we made here to that production app.
   - Any modifications to the database schema, data syncing logic, or data restoration logic MUST be strictly backward compatible. If a field name is changed, a proper migration must be mapped so older data is gracefully transitioned.
   - Data restoration logic must gracefully handle missing fields in older Firebase documents without crashing or losing records.