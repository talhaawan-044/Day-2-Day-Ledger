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
   - The user will drop UI screenshots via KDE connect into `/mnt/DEVELOPMENT/Day-2-Day-Kotlin-Final/App-screenshots-for-ai/` for AI agents to review.
   - **CRITICAL:** Always check this directory when the user mentions screenshots. Once the screenshot has been analyzed and the corresponding task is complete, the AI **MUST** delete the screenshot to keep the directory clean.
