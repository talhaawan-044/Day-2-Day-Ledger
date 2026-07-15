# Day 2 Day Ledger

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Room Database](https://img.shields.io/badge/Room-Local_Storage-green?style=for-the-badge)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

Day 2 Day Ledger is a comprehensive, local-first business management application designed primarily for operations involving extensive inventory tracking (such as coal mines), financial ledgers, and vehicle fleet management. Built with modern Android development practices, it leverages Kotlin and Jetpack Compose to deliver a robust, highly responsive user interface while ensuring data persistence through Room Database and seamless cloud backups via Firebase.

This application is engineered for business owners who require a reliable tool to track daily financial operations, manage supplier and buyer networks, monitor inventory metrics, and organize critical business notes.

## Table of Contents
- [Core Features](#core-features)
- [Application Interface](#application-interface)
- [Technical Architecture](#technical-architecture)
- [Data Privacy & Security](#data-privacy--security)
- [Data Export & Backup](#data-export--backup)
- [Installation & Setup](#installation--setup)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Core Features

### 1. Financial Ledger & Contacts Management
The backbone of the application is a fully-featured financial ledger that tracks net market credit, receivables, and payables. 
- **Parties Module:** Manage a comprehensive list of contacts, categorized as Buyers or Suppliers.
- **Financial Insights:** Access real-time insights into total incoming revenue, monthly operating expenses, and overall balances.
- **Transaction History:** Maintain a precise, immutable record of all financial transactions associated with specific parties.

### 2. Inventory & Stock Management
Specifically tailored for industrial use cases such as coal mining and logistics.
- **Coal Mine Initialization:** Set up dedicated repositories for different coal mines or material types.
- **Volume Tracking:** Log initial weights and track subsequent additions or reductions in tons.
- **Stock Filtering:** Quickly search and filter through multiple active mines to determine current resource availability.

### 3. Vehicle Tracker (Garage)
A dedicated module for logistics and fleet management.
- **Vehicle Profiles:** Add new vehicles specifying nicknames, plate numbers, and vehicle types (Truck, Car).
- **Odometer Tracking:** Monitor the initial and current odometer readings in kilometers.
- **Maintenance Intervals:** Configure custom service intervals (e.g., oil changes every 4000 km) and receive notifications when a vehicle is due for maintenance.

### 4. Expense Tracking
Categorize and monitor daily business expenditures to maintain strict budget controls.
- **Categorization:** Predefined categories such as Food, Transport, and Business expenses.
- **Detailed Logging:** Attach dates, amounts, and optional descriptive notes to every expense entry.
- **Time-bound Reporting:** View expenses filtered by recent transactions or specific periods.

### 5. Notes & Folders
An integrated rich-text editor designed for maintaining operational notes, meeting minutes, and internal memos.
- **Folder Organization:** Group notes into logically structured folders.
- **Rich Editing:** A robust editor screen that supports comprehensive text formatting.

### 6. Reminders & Alarms
A built-in scheduling system ensuring critical business tasks are never missed.
- **Alarm Sound Manager:** Native integration with Android's alarm system for high-priority alerts.
- **Scheduled Notifications:** Leverage the internal `ReminderScheduler` and `NotificationHelper` to set custom triggers.

## Application Interface

The application features a dark-themed, modern interface built entirely with Jetpack Compose. Below is an overview of the core application screens.

### Dashboard & Settings
| Home Dashboard | Settings Overview | Privacy & Preferences |
|:---:|:---:|:---:|
| <img src="Screenshots/Home_Dashboard_Screen.jpg" width="250"> | <img src="Screenshots/Settings_Top_Screen.jpg" width="250"> | <img src="Screenshots/Settings_Bottom_Screen.jpg" width="250"> |

### Contacts & Finances
| Contacts & Parties | Add New Party | Expenses Tracker |
|:---:|:---:|:---:|
| <img src="Screenshots/Contacts_Parties_Screen.jpg" width="250"> | <img src="Screenshots/Add_Party_BottomSheet.jpg" width="250"> | <img src="Screenshots/Expenses_Screen.jpg" width="250"> |

### Inventory & Operations
| Inventory Dashboard | Initialize Mine | Notes & Folders |
|:---:|:---:|:---:|
| <img src="Screenshots/Inventory_Stock_Screen.jpg" width="250"> | <img src="Screenshots/Inventory_Stock_Screen.jpg" width="250"> | <img src="Screenshots/Notes_Folders_Screen.jpg" width="250"> |

### Fleet Management
| Garage Overview | Add Vehicle Details | Add Expense Details |
|:---:|:---:|:---:|
| <img src="Screenshots/Garage_Vehicle_Screen.jpg" width="250"> | <img src="Screenshots/Add_Vehicle_BottomSheet.jpg" width="250"> | <img src="Screenshots/Add_Expense_BottomSheet.jpg" width="250"> |

## Technical Architecture

The application is built utilizing modern Android architecture components to ensure scalability, testability, and a seamless user experience.

- **Design Pattern:** Model-View-ViewModel (MVVM) architecture ensures a clean separation of concerns.
- **UI Framework:** 100% Jetpack Compose for declarative UI development. State hoisting and unidirectional data flow are strictly implemented.
- **Concurrency:** Kotlin Coroutines and `Flow` (specifically `StateFlow` and `SharedFlow`) are utilized for asynchronous data streams and reactive UI updates.
- **Local Persistence:** Room Database serves as the single source of truth, providing offline-first capabilities.
- **Background Processing:** `WorkManager` (e.g., `BackupWorker.kt`) handles deferred, guaranteed background tasks such as remote synchronization.
- **Dependency Injection:** Dagger Hilt is implemented for scalable dependency management.

## Data Privacy & Security

Given the sensitive nature of financial and business data, the application implements stringent security protocols:

- **Local-First Architecture:** All primary data operations are executed locally on the device, minimizing network exposure.
- **Application Lock:** A robust lock screen restricts access to authorized users.
- **Biometric Authentication:** Integration with Android's BiometricPrompt supports TouchID and FaceID.
- **PIN Protection:** Fallback custom PIN configuration available in the privacy settings.
- **Guest Mode:** Safe data exploration utilizing isolated guest instances.

## Data Export & Backup

The application provides extensive utilities (`utils/ExportUtils.kt`, `utils/XlsxWriter.kt`) for data portability and backup:

- **PDF Generation:** Native rendering of reports to PDF documents utilizing the Android `PdfDocument` framework.
- **Excel Export:** Generation of `.xlsx` spreadsheet files utilizing Apache POI for deep data analysis.
- **Cloud Backup:** Manual and automated backup snapshots utilizing Firebase services for secure, remote data storage.
- **Data Restoration:** Seamless recovery capabilities from existing snapshots.

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/Day-2-Day-Ledger.git
   ```
2. **Open in Android Studio:**
   Navigate to `File > Open` and select the cloned directory.
3. **Configure Firebase:**
   - Create a new project in the Firebase Console.
   - Register the Android app and download the `google-services.json` file.
   - Place the `google-services.json` file in the `app/` directory.
   - *Note: `google-services.json` is explicitly ignored by `.gitignore` to prevent credential leakage.*
4. **Build and Run:**
   Sync the project with Gradle files and deploy to an emulator or physical device.

## Project Structure

```text
app/src/main/java/com/example/awancoalledger/
├── ui/
│   ├── screens/       # Jetpack Compose UI Screens (e.g., LedgerDetailScreen.kt, InventoryScreen.kt)
│   ├── components/    # Reusable UI widgets
│   └── theme/         # Color palettes, typography, and shape definitions
├── viewmodel/         # Business logic and state management (e.g., LedgerViewModel.kt)
├── utils/             # Helper classes (e.g., ExportUtils, DataExchangeUtils, AlarmSoundManager)
├── workers/           # WorkManager definitions (e.g., BackupWorker.kt)
└── data/              # Room DAOs, Entities, and Repository implementations
```

## Contributing

While this project is primarily maintained for personal business operations, contributions that enhance the architecture, add significant features, or resolve critical bugs are welcome. Please ensure that any pull requests adhere to the existing MVVM structure and include appropriate Compose previews.

## License

Copyright © 2026. All rights reserved. 
This software and associated documentation files are proprietary and confidential. Unauthorized copying, distribution, or modification of this project, via any medium, is strictly prohibited.
