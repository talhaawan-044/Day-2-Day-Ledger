# Day 2 Day Ledger (v2.0)

A comprehensive, offline-first business management application built for the modern coal trading industry. Day 2 Day Ledger replaces paper khatas and scattered spreadsheets with a single, secure Android app. Featuring a stunning, premium iOS-inspired aesthetic, it handles party ledgers, inventory tracking, expense management, fleet maintenance, reminders, and notes -- all synced to the cloud when you choose.

<p align="left">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Database-Room_SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white" />
  <img src="https://img.shields.io/badge/Min_SDK-26-10B981?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Target_SDK-35-3B82F6?style=for-the-badge" />
</p>

---

## What's New in Version 2.0

We have completely reimagined the application from the ground up to deliver a world-class, premium user experience alongside crucial data safety features.

- **Premium Visual Overhaul:** A completely new iOS-inspired design system featuring frosted glass effects, buttery smooth micro-animations, curated typography, and meticulously crafted layouts. 
- **Soft-Delete Data Safety:** Accidental deletions are a thing of the past. Deleted records are now gracefully moved to a secure 'Trash' state, allowing for instant recovery and complete peace of mind.
- **Global Country Code Support:** A sleek, fully searchable country code selector for international numbers, including a 'Custom' entry option to ensure total global compatibility.
- **Intelligent WhatsApp Integration:** Sharing invoices and transaction slips now automatically includes the party's name and relevant context directly in the message header.
- **Optimized Screen Real Estate:** Redesigned summary cards, cleaner grid views, and refined spacing ensure maximum data visibility without feeling cluttered.

---

## Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Core Modules](#core-modules)
- [Export & Sharing](#export--sharing)
- [Cloud Sync and Backup](#cloud-sync-and-backup)
- [Security](#security)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [License](#license)

---

## Overview

Day 2 Day Ledger was built to solve the complex daily financial operations of a coal trading business. It flawlessly manages truck deliveries (weight, rate, and fare), bi-directional payments between buyers and suppliers, and distributed inventory tracking across multiple mines and warehouses. 

Operating entirely offline via Room Database as the single source of truth, it ensures lightning-fast performance. When connected to a cloud account, all data synchronizes to Firebase Firestore in real-time for seamless backup and multi-device capabilities.

---

## Screenshots

### Dashboard and Settings

| Home Dashboard | Settings (Cloud and Profile) | Settings (Security and Data) |
|:-:|:-:|:-:|
| <img src="Screenshots/Home_Dashboard_Screen.jpg" width="250"/> | <img src="Screenshots/Settings_Top_Screen.jpg" width="250"/> | <img src="Screenshots/Settings_Bottom_Screen.jpg" width="250"/> |

### Contacts and Expenses

| Parties / Contacts | Add New Party | Expense Tracker |
|:-:|:-:|:-:|
| <img src="Screenshots/Contacts_Parties_Screen.jpg" width="250"/> | <img src="Screenshots/Add_Party_BottomSheet.jpg" width="250"/> | <img src="Screenshots/Expenses_Screen.jpg" width="250"/> |

### Inventory and Notes

| Add New Expense | Inventory / Stock | Initialize Mine |
|:-:|:-:|:-:|
| <img src="Screenshots/Add_Expense_BottomSheet.jpg" width="250"/> | <img src="Screenshots/Inventory_Stock_Screen.jpg" width="250"/> | <img src="Screenshots/Initialize_Mine_BottomSheet.jpg" width="250"/> |

### Fleet and Notes

| Notes / Folders | Vehicle Garage | Add New Vehicle |
|:-:|:-:|:-:|
| <img src="Screenshots/Notes_Folders_Screen.jpg" width="250"/> | <img src="Screenshots/Garage_Vehicle_Screen.jpg" width="250"/> | <img src="Screenshots/Add_Vehicle_BottomSheet.jpg" width="250"/> |

> **Note:** Screenshots are continually updated to reflect the latest v2.0 design language.

---

## Core Modules

### Home Dashboard
The financial command center of the application, greeting the user with context-aware timing.
- **Net Market Credit:** Instantly view the difference between total receivables and payables, complete with Surplus/Deficit indicators.
- **Financial Insights:** Horizontally scrollable cards detailing incoming funds, monthly operating expenses, and daily spending.
- **Vehicle Status:** Real-time mileage tracking indicating distance until the next required oil change.
- **Recent Activity Feed:** A quick-glance list of the latest 10 ledger entries and payments, instantly tappable for full details.

### Party Ledger
The heart of the financial tracking system, managing both Buyers and Suppliers with distinct logical flows.
- **Contacts Management:** Grid and list views, search filtering, and swipe actions.
- **Transaction Records:** Detailed entry forms for coal deliveries including Truck Number, Mine, Warehouse, Weight, Rate, Fare, and Advance Payments.
- **Intelligent Balancing:** Automatic calculation of net balances based on party type and transaction history.
- **Soft Delete:** Safely remove ledgers and entries with the ability to instantly restore them from the trash.

### Inventory and Stock
Purpose-built for managing bulk commodities across multiple geographic locations.
- **Mine Initialization:** Create distinct repositories with starting tonnages.
- **Aggregate Tracking:** Global view of all stock across the entire business.
- **Historical Reference:** Track peak weights and individual stock movements over time.

### Expense Tracker
Categorized daily business and personal expenditure tracking.
- **Dynamic Filtering:** Sort by Transport, Business, Utilities, Food, or Others.
- **Automated Totals:** Daily and monthly sums calculated instantly.

### Vehicle Garage
A complete fleet management suite for business vehicles.
- **Multi-Vehicle Support:** Track cars and trucks independently.
- **Comprehensive Logging:** Record fuel (with calculated efficiency metrics), oil changes, and general maintenance.
- **Predictive Alerts:** Automatic notifications when an oil change is approaching or overdue based on fuel log mileage.

### Notes and Folders
A deeply integrated, rich-text note-taking system.
- **Rich Editor:** Adjust fonts, background patterns, and text colors. 
- **Robust Tools:** 50-level undo/redo history and 600ms debounce auto-saving.

### Reminders
Business-oriented task management with native alarm integration.
- **Priority & Categorization:** Visual indicators for task urgency and type.
- **Native Scheduling:** Utilizes Android's `AlarmManager` for exact, reliable notifications and snooze functionality.

---

## Export & Sharing

Professionalism is key when communicating with clients. Day 2 Day Ledger provides two independent, native export systems:

**PDF Engine:**
- Built purely on Android's `PdfDocument` API without heavy third-party libraries.
- Generates multi-page, branded documents with company logos, signature blocks, and alternating row colors.
- Color-coded financial data for immediate readability.

**Excel (.xlsx) Engine:**
- A custom-built, pure Kotlin OOXML writer -- extremely lightweight with no Apache POI dependencies.
- Produces heavily styled workbooks featuring neon-green accents, proper borders, and distinct summary rows.

**WhatsApp Integration:**
- Instantly share generated reports or individual transaction slips directly to WhatsApp, complete with intelligently pre-filled messages containing the party's name and transaction details.

---

## Cloud Sync and Backup

A robust three-phase architecture ensures data is always safe without ever interrupting the user workflow.

1. **Guest Mode:** Silent anonymous authentication allows immediate, local-only usage.
2. **Cloud Registration:** Link to Google or Email/Password to automatically begin real-time Firestore synchronization.
3. **Conflict Resolution:** Intelligent handling of existing accounts, offering seamless restoration of cloud data.

- **Real-Time Engine:** A highly optimized `SyncManager` handles per-entity background syncing via Firestore listeners.
- **Local Snapshots:** Automatic JSON-based local backups stored securely on the device, retaining the 5 most recent snapshots.

---

## Security

Your financial data is sensitive. The application provides multiple layers of protection:
- **App Lock:** A custom 4-digit PIN system.
- **Biometric Integration:** Native support for Fingerprint/Face Unlock via Android's `BiometricPrompt`.
- Lock screen enforcement immediately triggers upon app resume.

---

## Architecture & Tech Stack

Following modern Android development best practices, the application utilizes a reactive **MVVM (Model-View-ViewModel)** architecture. 

- **UI:** Jetpack Compose + Material 3
- **Local Persistence:** Room (SQLite) with KSP
- **Cloud Backend:** Firebase (Firestore, Auth, Storage)
- **Concurrency:** Kotlin Coroutines & Flow
- **Background Tasks:** WorkManager
- **Image Handling:** Coil

The single source of truth is the Room database. All write operations follow a strict `write-local-then-sync` pattern, ensuring the UI remains incredibly responsive and fully functional without an internet connection.

---

## Project Structure

```text
app/src/main/java/com/example/awancoalledger/
|-- data/
|   |-- DataModels.kt         # 13 Room entities (Party, LedgerEntry, Payment, Expense, Stock, etc.)
|   |-- Enums.kt              # Shared enumerations
|   |-- LedgerDao.kt          # Room DAO with Flow-based queries
|   |-- LedgerDatabase.kt     # Room database definition
|   |-- LedgerRepository.kt   # Repository pattern wrapping DAO
|   |-- SettingsRepository.kt # SharedPreferences wrapper with Flow support
|   |-- FirebaseManager.kt    # Firebase Auth and Storage operations
|   |-- SyncManager.kt        # Real-time Firestore sync engine (48KB)
|   '-- Converters.kt         # Room type converters
|
|-- viewmodel/
|   |-- LedgerViewModel.kt    # Central ViewModel (1259 lines, all business logic)
|   '-- LedgerViewModelFactory.kt
|
|-- ui/
|   |-- screens/
|   |   |-- SummaryScreen.kt          # Home dashboard (1072 lines)
|   |   |-- PartiesScreen.kt          # Contact management
|   |   |-- LedgerDetailScreen.kt     # Per-party transaction ledger (980 lines)
|   |   |-- ExpensesScreen.kt         # Expense tracking
|   |   |-- InventoryScreen.kt        # Coal stock management
|   |   |-- StockDetailScreen.kt      # Per-mine stock detail
|   |   |-- VehicleTrackerScreen.kt   # Fleet management (1010 lines)
|   |   |-- NotesScreen.kt            # Notes listing
|   |   |-- NoteEditorScreen.kt       # Rich text editor (1753 lines)
|   |   |-- FoldersScreen.kt          # Folder management
|   |   |-- RemindersScreen.kt        # Reminder management
|   |   |-- SettingsScreen.kt         # App configuration (1201 lines)
|   |   |-- LoginScreen.kt            # Auth dialog (Google + Email)
|   |   '-- LockScreen.kt             # PIN and biometric lock
|   |-- components/                   # Reusable Compose widgets
|   '-- theme/                        # Color, typography, and shape tokens
|
|-- utils/
|   |-- ExportUtils.kt        # Native PDF generation (1125 lines)
|   |-- XlsxWriter.kt         # Pure Kotlin .xlsx writer (224 lines)
|   |-- DataExchangeUtils.kt  # JSON backup serialization and Base64 image encoding
|   |-- ReminderScheduler.kt  # AlarmManager integration for reminder alarms
|   |-- NotificationHelper.kt # Android notification channel and delivery
|   |-- AlarmSoundManager.kt  # Alarm audio playback
|   |-- DateUtils.kt          # Date formatting helpers
|   '-- Extensions.kt         # Kotlin extension functions
|
'-- workers/
    '-- BackupWorker.kt       # WorkManager periodic backup task
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/talhaawan-044/Day-2-Day-Ledger.git
   ```
2. Open the project in Android Studio.
3. **Configure Firebase:**
   - Create a project in the Firebase Console.
   - Enable Authentication (Anonymous, Google, Email).
   - Create a Firestore database and Storage bucket.
   - Download your `google-services.json` and place it in the `app/` directory. *(Note: This file is intentionally git-ignored).*
4. Sync Gradle and run on a physical device or emulator (API 26+).

---

## License

Copyright 2026 Awan Coal Ledger. All rights reserved.

This software is provided for personal and educational use. Redistribution or commercial use without explicit permission is prohibited.
