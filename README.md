# Day 2 Day Ledger

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Database-Room_&_Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Database" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</div>

<br/>

> A robust, production-grade Android application designed to facilitate comprehensive financial tracking and inventory management for individuals and small-to-medium enterprises.

---

## Table of Contents
1. [Core Philosophy](#core-philosophy)
2. [Key Features](#key-features)
3. [Technical Architecture](#technical-architecture)
4. [Security & Authentication](#security--authentication)
5. [Setup & Installation](#setup--installation)
6. [Data Synchronization](#data-synchronization)
7. [Contributing Guidelines](#contributing-guidelines)
8. [License](#license)

---

## Core Philosophy

The Day 2 Day Ledger is built on three foundational pillars:
- **Scalability**: Designed to handle growing datasets without performance degradation.
- **Maintainability**: Strictly adhering to Clean Architecture principles for a highly readable and testable codebase.
- **Data Security**: Prioritizing offline-first local persistence combined with secure cloud synchronization.

---

## Key Features

The application provides a meticulously organized system for managing daily operations.

### 1. Advanced Ledger Management
| Capability | Description |
| :--- | :--- |
| **Tracking** | Monitor payables and receivables across multiple parties. |
| **Granularity** | Log transactions with timestamps, related inventory items, and supplementary notes. |
| **Transaction Types** | Native support for standard accounting paradigms (Credit, Debit, Adjustments). |

### 2. Inventory and Stock Tracking
| Capability | Description |
| :--- | :--- |
| **Cataloging** | Maintain a detailed catalog of stock items with base configurations. |
| **Real-time Valuation** | Track quantities, unit prices, and overall stock valuation instantly. |
| **Movement Logging** | Support for complex stock movements (incoming shipments, outgoing sales). |

### 3. Party and Contact Management
Maintain a robust directory of all business associates (suppliers, customers, contractors). Each profile acts as a centralized hub for their contact information, complete financial history, and outstanding balances.

### 4. Professional Export Mechanisms
Generate highly-formatted, native reports directly on the device for immediate sharing.
- **PDF Documents**: Utilize native Android `PdfDocument` APIs for pixel-perfect printing.
- **Excel Spreadsheets**: Leverage Apache POI for structured `XLSX` generation.

### 5. Secure Offline-First Architecture
Function flawlessly in entirely disconnected environments. Using a sophisticated local Room database layer, all data is instantly accessible. When network connectivity is established, a background engine seamlessly coordinates with the remote cloud database.

### 6. Vehicle and Fleet Tracking
An integrated logistics module for tracking vehicle expenses, maintenance logs, and dispatch histories, keeping logistical operations financially transparent.

---

## Technical Architecture

Constructed following the principles of Clean Architecture and the Model-View-ViewModel (MVVM) pattern.

### Technology Stack
Below is a breakdown of the primary technologies utilized within the project.

| Domain | Technology / Library |
| :--- | :--- |
| **Language** | Kotlin (Version 2.0+) |
| **User Interface** | Jetpack Compose (Material Design 3) |
| **Local Persistence** | Room Database (SQLite abstraction) |
| **Cloud Infrastructure** | Firebase (Auth, Firestore, Storage) |
| **Concurrency** | Kotlin Coroutines, StateFlow |
| **Export Engines** | Apache POI, Android `PdfDocument` |
| **Background Tasks** | Android WorkManager |

<details>
<summary><b>Click to expand: Directory Structure</b></summary>

The project strictly follows a feature-by-feature directory structure within the main module.

```text
app/src/main/java/com/example/awancoalledger/
├── data/
│   ├── local/          (Room DAOs, Entities, and Database Configuration)
│   ├── remote/         (Firebase Firestore interfaces and models)
│   ├── repository/     (Single source of truth data mediation)
│   └── sync/           (Background synchronization logic)
├── ui/
│   ├── components/     (Reusable Jetpack Compose UI elements)
│   ├── screens/        (Full-screen Compose destinations)
│   └── theme/          (Color palettes, typography, styling tokens)
├── viewmodel/          (State holders and UI logic coordinators)
├── model/              (Domain-level data classes and enumerations)
└── utils/              (Helper functions, extensions, and export logic)
```
</details>

---

## Security & Authentication

Data integrity and user privacy are paramount.

- **Cloud Verification**: Utilizes Firebase Authentication to verify user identities.
- **Local Biometrics**: Supports on-device biometric authentication (fingerprint/facial recognition) to secure access to sensitive financial data.
- **Tenant Isolation**: Remote data transmitted to Firestore is secured via robust security rules ensuring users can only access their specific tenant's data.

---

## Setup & Installation

To build and run the application from source, follow these rigorous configuration steps.

### Prerequisites
1. Android Studio (Ladybug or newer recommended).
2. Java Development Kit (JDK) 21.
3. A Google account to provision a Firebase project.

### Step 1: Clone the Repository
Begin by cloning the source code to your local development environment using Git.
```bash
git clone https://github.com/yourusername/Day-2-Day-Ledger.git
cd Day-2-Day-Ledger
```

### Step 2: Configure Firebase Services
This repository does not include the proprietary `google-services.json` configuration file, as it contains private API keys and identifiers.

1. Navigate to the Firebase Console (https://console.firebase.google.com/).
2. Create a new Firebase Project.
3. Register a new Android application within the project using the package name `com.example.awancoalledger`.
4. Download the generated `google-services.json` file.
5. Place the `google-services.json` file directly into the `app/` directory of the cloned repository.

*Note: The `.gitignore` file is configured to prevent accidental commits of this sensitive file.*

### Step 3: Enable Firebase Features
Within your new Firebase project console, ensure the following services are enabled:
- **Authentication**: Enable the Email/Password sign-in provider.
- **Firestore Database**: Create a new Firestore database in production mode.
- **Firebase Storage**: Initialize the storage bucket for handling media and attachments.

### Step 4: Build and Deploy
1. Open the project in Android Studio.
2. Allow the Gradle sync process to complete.
3. Select an Android Virtual Device (AVD) or connect a physical device.
4. Execute `./gradlew installDebug` from the terminal (or click Run).

---

## Data Synchronization

The application utilizes a sophisticated local-first synchronization strategy.

1. **Local Writes**: All reads and writes are performed immediately against the local Room database, ensuring zero-latency UI interactions.
2. **Background Queue**: A continuous synchronization manager monitors local changes and queues them for remote transmission to Firestore.
3. **Offline Caching**: If the device loses internet connectivity, operations are safely cached locally.
4. **Conflict Resolution**: Upon network restoration, the WorkManager orchestrates a reliable synchronization pass, resolving any potential data conflicts using timestamp-based reconciliation.

---

## Contributing Guidelines

Contributions are highly encouraged, provided they adhere to the project's strict quality standards.

1. **Fork the Repository**: Create your own fork of the main repository.
2. **Create a Feature Branch**: Branch off from `main`.
    ```bash
    git checkout -b feature/advanced-analytics
    ```
3. **Adhere to Architecture**: Ensure new features maintain the separation between the UI layer, ViewModel layer, and Data layer. Unidirectional Data Flow (UDF) is strictly enforced.
4. **Formatting**: Format all Kotlin code according to the standard ktlint rules.
5. **Commit Messages**: Write clear, concise, and descriptive commit messages explaining the rationale behind the changes.
6. **Pull Requests**: Submit a pull request detailing the changes. All pull requests will be rigorously reviewed before merging.

---

## License

This project is licensed under the MIT License. You are free to use, modify, and distribute this software in accordance with the terms of the license.

Copyright (c) 2026. All rights reserved.

<br/>
<p align="center">
  <i>Developed with precision for daily financial operations.</i>
</p>
