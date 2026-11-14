<<<<<<< HEAD
# 🍃 SpendWise — Smart Personal & Student Expense Tracker

> **An elegant, privacy-first personal finance and student expense management application for Android, featuring an editorial "French Classic" design system, strict multi-user data isolation, and a next-generation multimodal Vision AI receipt parser.**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-purple.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Vision AI](https://img.shields.io/badge/AI%20Engine-Google%20Gemini%201.5%20Flash-blueviolet.svg)](https://aistudio.google.com)
[![Offline ML](https://img.shields.io/badge/Offline%20ML-Google%20ML%20Kit%20OCR-teal.svg)](https://developers.google.com/ml-kit)

---

## 📑 Project Structure & Milestone Overview

This project is divided into two clearly separated and modular activities:

| Activity | Focus Area | Description |
| :--- | :--- | :--- |
| **Activity 1** | **Core Financial Management System** *(Non-AI)* | Complete MVVM architecture, Room database with multi-user isolation, Jetpack Compose UI, authentication, 4-digit PIN/biometric app lock, custom financial keypad, category budgets, balance curves, and visual analytics. |
| **Activity 2** | **Intelligent Multimodal Vision AI Pipeline** *(AI Only)* | Cloud multimodal Google Gemini 1.5 Flash Vision engine, automated receipt & ticket parsing, zero-hallucination semantic extraction, on-device Google ML Kit OCR fallback, batch review UI, and in-app AI configuration. |

---

# 🏛️ ACTIVITY 1: Core Financial Management System (Non-AI)

Activity 1 delivers a complete, secure, robust, and offline-capable personal financial ledger designed specifically for students and young adults.

```
                              ACTIVITY 1 ARCHITECTURE
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│  [HomeScreen]  [AddTransactionScreen]  [TransactionsScreen]  [Budget]  │
│         [InsightsScreen]  [AuthScreen]  [AppLockScreen]  [Settings]    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlow / Events
┌───────────────────────────────────▼────────────────────────────────────┐
│                    SpendWiseViewModel (State Layer)                    │
│      Coroutines • StateFlow • UI State Management • Session Scope      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Repository Calls
┌───────────────────────────────────▼────────────────────────────────────┐
│                  SpendWiseRepository (Data Orchestration)              │
│       User Ownership Enforcement • Budget Calculations • Validation    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Local Queries (User Filtered)
┌───────────────────────────────────▼────────────────────────────────────┐
│                       Room Database (AppDatabase)                      │
│     [UserDao]     [TransactionDao]     [BudgetDao]     [UserSettings]  │
│                                                                        │
│                100% On-Device Encrypted Local Storage                  │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.1. User Authentication & Strict Multi-User Data Isolation
- **Registration & Login**: Secure account creation with user display name, normalized email address (case-insensitive and trimmed), and SHA-256 salted password hashing.
- **Strict Data Isolation**: Every transaction, budget limit, and preference is strictly bound by foreign key to the logged-in user's unique `userId`. 
- **Zero Cross-Account Leakage**: When User A logs out and User B logs in, all in-memory flows, ViewModels, and Room queries filter strictly by User B's `userId`. No previous user transactions or demo entries ever leak across accounts.
- **1-Tap Demo Mode**: Quick evaluation profile ("Maya Chen") for demonstration and grading purposes without creating new credentials.

### 1.2. Bank-Grade Security & App Lock
- **4-Digit Launch PIN**: Optional high-security PIN prompt requested every time the app launches or returns from the background.
- **Biometric Authentication**: Fingerprint and Face Unlock integration powered by `androidx.biometric.BiometricPrompt`.
- **Session Timeout & In-Memory Lock**: Automatic locking when the application lifecycle transitions to `STOPPED` or after background inactivity.

### 1.3. Interactive Financial Dashboard (HomeScreen)
- **Hero Balance Card**: Displays net balance with dynamic time-range filtering:
  - `1W` (Current Week)
  - `1M` (Current Month)
  - `3M` (Quarterly)
  - `All` (Lifetime Ledger)
- **Live Trend Curve**: Smooth Bezier curve visualizing historical spending fluctuations over time.
- **Income & Expense Summary Cards**: Quick-glance cards showing total income (allowance + part-time jobs) versus current expenditures.
- **Monthly Budget Meter**: Progress bar with dynamic color indicators:
  - *Sage Green*: On Track (< 70% budget consumed)
  - *Warm Ochre*: Nearing Limit (70% - 90%)
  - *Terracotta Red*: Exceeded (> 100%)
- **Recent Activity Ledger**: Chronological transaction feed with category icons and timestamps.

### 1.4. Manual Entry with Custom Keypad
- **Purpose-Built Keypad**: Eliminates the clunky standard Android soft keyboard in favor of a responsive, large-target financial keypad.
- **Category Taxonomy**: Pre-categorized pills:
  - `Food & Dining` • `Transport & Commute` • `Education & Books`
  - `Shopping` • `Entertainment` • `Health` • `Bills & Utilities` • `Other`
- **Payment Method Tracking**: Supports `UPI`, `Cash`, `Card`, and `Net Banking`.
- **Direction Toggle**: One-tap toggle between `Expense` (debit) and `Income` (credit).

### 1.5. Activity Ledger & Search Filtering
- Full-text search across transaction notes, payee descriptions, and categories.
- Filter chips: `All`, `Expenses Only`, `Income Only`.
- Chronological sorting: `Newest First`, `Oldest First`, `Highest Amount`, `Lowest Amount`.
- Swipe-to-delete or tap-to-edit with ownership validation.

### 1.6. Category Budgets & Spending Limits
- Custom monthly spending targets set per category (e.g., Food: ₹3,000, Transport: ₹2,000, Shopping: ₹1,500).
- Real-time remaining balance calculations (e.g., *"₹9,875 left of ₹10,000"*).
- Visual status indicators informing the user if spending pace is healthy or trending toward an early month-end deficit.

### 1.7. Visual Analytics & Insights
- **Spending Donut Chart**: High-contrast proportional ring chart visualizing category distribution.
- **Weekly Bar Breakdown**: Day-by-day (Monday through Sunday) expense volume bars.
- **Contextual Financial Advice**: Dynamic smart cards (e.g., *"Bills is your highest spending category this month"*).

### 1.8. Design System: French Classic / Quiet Luxury Aesthetic
- **Color Palette**:
  - `Dark Pine` (`#1C3B2B`): Core branding and surface emphasis.
  - `Terracotta Rust` (`#C85A32`): Accent highlights and alert indicators.
  - `Pale Green / Sage` (`#E2ECE9` / `#94A89A`): Soft backgrounds and progress tracks.
  - `Antique Gold` (`#D4AF37`): Hairline rules and premium badges.
  - `French Cream / Ivory` (`#F9F6F0`): Paper-like eye-friendly canvas.
- **Typography**: High-contrast editorial serif headers (`Georgia` / `Playfair Display`) paired with clean geometric sans-serif for numbers and metadata.

---

# 🤖 ACTIVITY 2: Intelligent Multimodal Vision AI Pipeline (AI Only)

Activity 2 elevates SpendWise into an autonomous expense assistant. Instead of typing amounts and stores manually, users can simply drop screenshots of bus tickets, UPI payments, or food deliveries.

```
                              ACTIVITY 2 PIPELINE
┌────────────────────────────────────────────────────────────────────────┐
│                        User Receipt / Screenshot                       │
│    [BMTC Bus Ticket]   [GPay / PhonePe UPI]   [Swiggy / Zomato Bill]   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Single / Multi-Select
┌───────────────────────────────────▼────────────────────────────────────┐
│                    Image Acquisition & Pre-Processing                  │
│    EXIF Rotation Correction • Subsampling (1600px Max) • AR Preserved  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  │ Network & API Key Available?      │
                  └─────────┬───────────────────┬─────┘
                     YES    │                   │   NO / Offline
┌───────────────────────────▼─────────┐   ┌─────▼────────────────────────┐
│   Primary: Google Gemini 1.5 Flash  │   │  Fallback: On-Device ML Kit  │
│  Multimodal Vision AI (Cloud LLM)   │   │  Local Offline Text OCR +    │
│  - Semantic Reasoning & Context     │   │  Regex Semantic Parser       │
│  - Route Number != Amount Rule      │   │  - 100% Offline Resilience   │
│  - Direct JSON Schema Enforcement   │   │  - Zero API Key Required     │
└───────────────────────────┬─────────┘   └─────┬────────────────────────┘
                            │                   │
                            └─────────┬─────────┘
                                      │ Parsed Transactions
┌─────────────────────────────────────▼──────────────────────────────────┐
│                     Screenshot Review & Verification                   │
│   Editable Amount • Category Dropdown • Date Picker • Delete Item      │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │ "Add Expense" One-Tap Import
┌─────────────────────────────────────▼──────────────────────────────────┐
│                   Persisted to Room Database (Activity 1)              │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1. Cloud Multimodal Vision Engine (Google Gemini 1.5 Flash)
- **Library**: `com.google.ai.client.generativeai:generativeai:0.9.0`
- **Model**: `gemini-1.5-flash`
- **Zero-Shot Understanding**: Unlike dumb regex OCR that reads every number indiscriminately, Gemini's visual reasoning understands layout context:
  - **Transit Tickets (e.g. BMTC, DTC, BEST)**: Understands that `Route 26` or `Bus No. 26` is an identifier, and correctly extracts the ticket fare `₹6.00`.
  - **UPI Screens (Google Pay, PhonePe, Paytm, BHIM, CRED)**: Accurately identifies the payee merchant (`To KHUSHI CHATS`), extracts the Bank RRN / Transaction ID (`26j29E...`), and selects the exact payment method.
  - **Food Delivery & Invoices (Swiggy, Zomato, Blinkit, Zepto)**: Extracts net paid total rather than subtotal discounts.
- **Strict JSON Schema Generation**: Configured with `responseMimeType = "application/json"` and temperature `0.1` for deterministic, structured JSON output:
  ```json
  [
    {
      "amount": 125.0,
      "description": "To KHUSHI CHATS",
      "category": "Bills",
      "type": "EXPENSE",
      "paymentMethod": "UPI",
      "referenceNumber": "26j29E1117866mdR",
      "date": 1730745000000
    }
  ]
  ```

### 2.2. Robust On-Device Fallback (Google ML Kit Text Recognition)
- **Library**: `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1`
- **Zero-Downtime Guarantee**: If the device is in airplane mode, has poor internet connectivity, or has reached its API rate limit, SpendWise **silently and automatically falls back** to the on-device ML Kit OCR parser.
- **Pattern Matching Heuristics**: Extracts currency symbols (`₹`, `INR`, `Rs.`), dates, transaction IDs, and maps merchants using keyword pattern matching.

### 2.3. Image Acquisition & Pre-Processing Pipeline
- **Memory Optimization**: Employs an in-memory two-pass sub-sampler (`decodeSampledBitmap`) that downsamples large 4K smartphone screenshots to a max dimension of 1600px, avoiding Android `OutOfMemoryError` (OOM).
- **EXIF Auto-Orientation**: Reads hardware orientation tags via `androidx.exifinterface:exifinterface` to prevent rotated receipt images from confusing the model.

### 2.4. Batch Screenshot Review Screen
- **Interactive Staging Buffer**: Scanned results are presented in a dedicated verification screen (`ScreenshotReviewScreen.kt`) before anything is saved to disk.
- **One-Tap Edits**: Users can adjust the amount, change the auto-selected category pill, edit merchant titles, or swipe away unintended receipts.
- **Single-Tap Batch Import**: Directly injects the validated items into the user's Room database via `SpendWiseViewModel.importParsedTransactions()`.

### 2.5. In-App AI Configuration & Key Management
- Located in **Settings ➔ AI & SMART DETECTION**:
  - **Google Gemini Vision AI Toggle**: Allows switching between Cloud Multimodal AI and local on-device OCR at will.
  - **Gemini API Key Dialog**: Allows users to enter their own custom API key from [Google AI Studio](https://aistudio.google.com).
  - **Fallback Status Badge**: Real-time status displaying *"ML Kit On-Device OCR Ready"*.

---

## 🛠️ Technology Stack & Dependencies

```
┌─────────────────────────┬────────────────────────────────────────────────────────┐
│ Layer                   │ Technology / Library                                  │
├─────────────────────────┼────────────────────────────────────────────────────────┤
│ Architecture            │ MVVM (Model-View-ViewModel), Clean Architecture        │
│ Language                │ Kotlin 2.0 (Coroutines, StateFlow, Flow)               │
│ UI Framework            │ Jetpack Compose (Material 3, Animations, Icons)        │
│ Local Database          │ Room 2.7 (SQLite ORM, TypeConverters, KSP)            │
│ Security & Auth         │ AndroidX Biometric, SHA-256 Crypto Hashing            │
│ Cloud Multimodal AI     │ Google Generative AI Client 0.9.0 (Gemini 1.5 Flash)   │
│ On-Device Machine Learn │ Google ML Kit Text Recognition 19.0.1                  │
│ Networking & JSON       │ OkHttp 4.12, Gson 2.11                                 │
│ Image Utilities         │ AndroidX ExifInterface, Android Photo Picker Contract │
│ Build System            │ Gradle 9.5 (KTS Version Catalogs)                      │
└─────────────────────────┴────────────────────────────────────────────────────────┘
```

---

## 🚀 Getting Started & Installation

### Option 1: Install Prebuilt APK Directly
If you just want to test or demo the app on an Android device (Android 7.0+ / API 24+):
1. **SpendWise-AI (With Gemini Vision AI & ML Kit)**:  
   👉 `SpendWise-AI/SpendWise-AI.apk` *(~61.8 MB)*
2. **SpendWise (Core App Only)**:  
   👉 `SpendWise/SpendWise.apk` *(~59.9 MB)*

*(Downloadable over local Wi-Fi when server is running: `http://<YOUR_IP>:9998/SpendWise-AI.apk`)*

### Option 2: Build from Source in Android Studio
1. Clone or open the project folder `SpendWise-AI` in **Android Studio (Ladybug or newer)**.
2. Ensure you have **JDK 21** configured (`File ➔ Settings ➔ Build, Execution, Deployment ➔ Build Tools ➔ Gradle ➔ Gradle JDK`).
3. Sync Gradle and run on an emulator (Pixel 4 or newer) or connected physical device.
4. Build via command line:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🔑 Setting Up Your Free Gemini API Key (For Activity 2)

SpendWise includes an embedded evaluation key, but you can also configure your own personal free key in 30 seconds:

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey) and sign in with your Google account.
2. Click **"Create API key"** and choose **"Default Gemini Project"** (100% free with 15 requests/minute).
3. Copy the generated key (starts with `AQ.` or `AIzaSy`).
4. In the app:
   - Tap **SETTINGS** (bottom-right tab).
   - Scroll to **AI & SMART DETECTION**.
   - Tap **"Gemini API Key"**, paste your key, and tap **Save**.
   - Ensure the **"Google Gemini Vision AI"** toggle is switched **ON**.

---

## 🧪 Comprehensive Verification & Test Suite

### Activity 1 Verification Scenarios:
- **Test 1.1: Multi-User Data Isolation**:
  Register User A ➔ Add 3 transactions ➔ Log out ➔ Register User B. Verify User B starts with zero transactions and empty charts.
- **Test 1.2: Session Security**:
  Enable "App Lock on Launch" in Settings. Minimize app and restore. Verify that the 4-digit PIN screen appears before showing financial data.
- **Test 1.3: Manual Entry Calculation**:
  Use custom keypad to log ₹150 for "Food" (Cash) and ₹350 for "Transport" (UPI). Verify that total balance, recent transactions, and category budgets update immediately.

### Activity 2 Verification Scenarios:
- **Test 2.1: Bus Ticket Route vs. Amount Differentiation**:
  Upload a screenshot of a BMTC bus ticket with route `26` and fare `₹6.00`. Verify that Gemini Vision AI detects amount `₹6.00` and category `Transport` instead of `₹26.00`.
- **Test 2.2: UPI Screenshot Batch Import**:
  Select a Google Pay or PhonePe payment screenshot. Verify that the Review screen extracts the exact payee name, amount, and payment method (`UPI`). Confirm to persist it to the Activity ledger.
- **Test 2.3: Offline Fallback Resilience**:
  Enable Airplane Mode on device. Upload a receipt screenshot. Verify that the app smoothly falls back to Google ML Kit on-device OCR without crashing or throwing unhandled errors.

---

## 🎥 Promotional & Showcase Video

A complete 52-second promotional video showcasing both Activity 1 and Activity 2 is included in the project root:
- **File**: `SpendWise_Promotional_Video.mp4`
- **Highlights**: High-definition phone mockups, animated Gemini AI scanlines, synchronized English voiceover narration, ducked lofi soundtrack, and closed captions.

---

## 📄 License & Academic Attribution
Developed as an academic Android project for **Advanced Mobile Software Engineering**. Open-sourced under the MIT License.
=======
# 💰 SpendWise

> A modern personal expense and budget tracking Android application built with Kotlin and Jetpack Compose.

SpendWise is an Android application developed to help users manage their personal finances in a simple and organized way. It allows users to record income and expenses, manage budgets, search and filter transactions, analyze spending patterns, and maintain their financial data locally using Room Database.

---

## 📱 Project Overview

Managing everyday expenses can become difficult when transactions are scattered across different applications or written down manually.

SpendWise provides a centralized mobile solution where users can:

- Track income and expenses
- Categorize transactions
- Monitor their current balance
- Set a monthly budget
- View recent transactions
- Search and filter transactions
- Sort transactions
- Analyze spending patterns
- View category-wise spending
- View daily and periodic spending
- Edit and delete transactions
- Manage student profile information
- Configure application preferences
- Export an expense report
- Reset the application with sample data
- Clear stored transaction data
- Switch between light and dark themes

All core financial data is stored locally on the device using Room Database.

---

## ✨ Key Features

### 🏠 Dashboard

The Home screen provides a quick financial overview.

It displays:

- Current balance
- Total income
- Total expenses
- Monthly budget
- Recent transactions
- Financial summary cards
- Spending overview

The dashboard is designed to give users important financial information at a glance.

---

### 💸 Add Income & Expenses

Users can create financial transactions by entering:

- Amount
- Transaction type
- Category
- Note/description
- Payment method
- Date

The application supports both:

- Income
- Expense

Transactions can be added through an intuitive custom input interface.

---

### 🧾 Transaction Management

The Transactions screen provides a complete list of stored transactions.

Users can:

- View transactions
- Search transactions
- Filter by transaction type
- Filter by category
- Sort transactions
- Open transaction details
- Edit transactions
- Delete transactions

The search functionality supports queries involving:

- Transaction notes
- Categories
- Transaction information

---

### 📊 Analytics

The Analytics screen provides a visual overview of spending behavior.

It includes:

- Total spending
- Category-wise spending
- Spending percentages
- Daily spending information
- Period-based analysis
- Spending charts
- Comparison information

Analytics help users understand their spending habits and identify major expense categories.

---

### 🎯 Budget Management

SpendWise allows users to set and manage a monthly budget.

The budget functionality provides:

- Monthly budget configuration
- Budget amount tracking
- Expense comparison
- Budget status
- Budget-related information

Example:

```text
Monthly Budget: ₹10,000
📤 Expense Report

SpendWise provides an expense report generation feature.

The report contains information such as:

Generated date
Total income
Total expenses
Current balance
Transaction details

The generated report can be copied to the device clipboard for easy sharing or storage.

🌙 Theme Support

The application supports theme customization including:

Light theme
Dark theme

The UI is implemented using Jetpack Compose and Material 3 components.

🛠️ Technology Stack
Technology	Purpose
Kotlin	Primary programming language
Jetpack Compose	Modern declarative UI
Material 3	UI components and design system
Navigation Compose	Screen navigation
Room Database	Local data persistence
SQLite	Local database engine
Kotlin Coroutines	Asynchronous operations
Kotlin Flow	Reactive data streams
StateFlow	UI state management
ViewModel	Application state and business logic
KSP	Room code generation
Gradle	Build automation
Android Studio	Development environment
🏗️ Architecture

SpendWise follows a layered architecture using ViewModel, Repository, DAO, and Room Database.

                    ┌──────────────────────┐
                    │    Jetpack Compose   │
                    │          UI          │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      ViewModel       │
                    │   UI State & Logic   │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │     Repository       │
                    │   Data Management    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │        DAO           │
                    │ Database Operations   │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    Room Database     │
                    │      SQLite           │
                    └──────────────────────┘
🔄 Data Flow
User Interaction
       ↓
Jetpack Compose Screen
       ↓
SpendWiseViewModel
       ↓
SpendWiseRepository
       ↓
Room DAO
       ↓
SQLite Database
       ↓
Flow / StateFlow
       ↓
Compose UI
       ↓
Updated Screen

This architecture separates the user interface, application logic, and data layer, making the project easier to maintain and extend.

🗂️ Project Structure
SpendWise/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       │
│   │       ├── java/
│   │       │   └── com/example/spendwise/
│   │       │       │
│   │       │       ├── MainActivity.kt
│   │       │       ├── SpendWiseApplication.kt
│   │       │       │
│   │       │       ├── data/
│   │       │       │   ├── entity/
│   │       │       │   │   ├── BudgetEntity.kt
│   │       │       │   │   ├── CategoryEntity.kt
│   │       │       │   │   ├── TransactionEntity.kt
│   │       │       │   │   └── UserSettingsEntity.kt
│   │       │       │   │
│   │       │       │   └── local/
│   │       │       │       ├── AppDatabase.kt
│   │       │       │       ├── BudgetDao.kt
│   │       │       │       ├── CategoryDao.kt
│   │       │       │       ├── TransactionDao.kt
│   │       │       │       └── UserSettingsDao.kt
│   │       │       │
│   │       │       ├── model/
│   │       │       │   ├── CategorySpending.kt
│   │       │       │   ├── DailySpending.kt
│   │       │       │   ├── PaymentMethod.kt
│   │       │       │   ├── PeriodType.kt
│   │       │       │   ├── SortOrder.kt
│   │       │       │   ├── TimeRange.kt
│   │       │       │   └── TransactionType.kt
│   │       │       │
│   │       │       ├── repository/
│   │       │       │   └── SpendWiseRepository.kt
│   │       │       │
│   │       │       ├── viewmodel/
│   │       │       │   └── SpendWiseViewModel.kt
│   │       │       │
│   │       │       ├── ui/
│   │       │       │   ├── components/
│   │       │       │   │   ├── BalanceCard.kt
│   │       │       │   │   ├── Charts.kt
│   │       │       │   │   ├── ConfirmationDialog.kt
│   │       │       │   │   ├── CustomKeypad.kt
│   │       │       │   │   ├── EmptyState.kt
│   │       │       │   │   ├── SpendWiseBottomBar.kt
│   │       │       │   │   ├── SpendWiseTopBar.kt
│   │       │       │   │   ├── SummaryCard.kt
│   │       │       │   │   └── TransactionItem.kt
│   │       │       │   │
│   │       │       │   ├── navigation/
│   │       │       │   │   ├── Screen.kt
│   │       │       │   │   └── SpendWiseNavGraph.kt
│   │       │       │   │
│   │       │       │   ├── screens/
│   │       │       │   │   ├── addtransaction/
│   │       │       │   │   ├── analytics/
│   │       │       │   │   ├── budget/
│   │       │       │   │   ├── home/
│   │       │       │   │   ├── settings/
│   │       │       │   │   └── transactions/
│   │       │       │   │
│   │       │       │   └── theme/
│   │       │       │       ├── Color.kt
│   │       │       │       ├── Theme.kt
│   │       │       │       └── Type.kt
│   │       │       │
│   │       │       └── util/
│   │       │           ├── CurrencyUtils.kt
│   │       │           └── DateUtils.kt
│   │       │
│   │       └── res/
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
🗄️ Database Design

SpendWise uses Room Database for local data persistence.

The main database entities are:

TransactionEntity

Stores individual income and expense transactions.

Transaction
├── ID
├── Transaction Type
├── Amount
├── Category
├── Note / Description
├── Date
└── Payment Method
BudgetEntity

Stores budget-related information.

Budget
├── ID
├── Month
├── Year
└── Budget Amount
CategoryEntity

Stores transaction categories.

Category
├── ID
├── Name
└── Type
UserSettingsEntity

Stores user preferences and profile information.

User Settings
├── ID
├── Currency
├── Student Name
├── College / Roll ID
├── Dark Mode
└── Budget Alerts
🧩 Main Application Modules
Data Layer

Responsible for persistent data storage.

data/
├── entity/
└── local/

The DAO classes provide database operations for:

Transactions
Categories
Budgets
User settings
Repository Layer
SpendWiseRepository.kt

The repository acts as the central data-access layer between the ViewModel and Room Database.

It manages:

Transactions
Budgets
Categories
User settings
Spending calculations
Analytics data
ViewModel Layer
SpendWiseViewModel.kt

The ViewModel manages application state and connects the UI with the repository.

It helps provide:

Reactive UI state
Transaction operations
Budget operations
Analytics data
User settings
Database updates
UI Layer

The UI is implemented completely using Jetpack Compose.

Main screens include:

Home
Transactions
Add Transaction
Analytics
Budget
Settings
🧭 Navigation

The application uses Navigation Compose.

The navigation flow can be represented as:

                    SpendWise
                       │
        ┌──────────────┼──────────────┐
        │              │              │
       Home       Transactions     Analytics
        │              │
        │              ├── Details
        │              ├── Edit
        │              └── Delete
        │
        ├── Add Transaction
        │
        ├── Budget
        │
        └── Settings
               │
               ├── Profile
               ├── Currency
               ├── Budget Alerts
               ├── Export Report
               ├── Reset Data
               └── Clear Data
💳 Payment Methods

Transactions support different payment methods through the PaymentMethod model.

Examples include:

UPI
Card
Cash
📁 Data Models

The application contains models for:

Transaction type
Payment method
Category spending
Daily spending
Period type
Sort order
Time range

These models help organize transaction processing and analytics.

🔍 Transaction Search & Filtering

The Transactions screen provides tools to efficiently find financial records.

Users can:

Search
  ↓
Filter by Type
  ↓
Filter by Category
  ↓
Sort Results
  ↓
View Transaction

This makes it easier to locate specific transactions even when many records are stored.

📈 Spending Analysis

SpendWise calculates and displays financial information such as:

Total Income
      ↓
Total Expenses
      ↓
Current Balance
      ↓
Category Spending
      ↓
Daily / Period Spending

The application can present spending information through charts and summary components.

🧪 Testing

The project includes Android testing dependencies such as:

JUnit
AndroidX JUnit
Espresso

Run unit tests using:

.\gradlew.bat test

Run connected Android tests using:

.\gradlew.bat connectedAndroidTest
💻 Requirements

To build and run SpendWise, install:

Android Studio
JDK 21
Android SDK
Android SDK Platform 36
Android device or emulator

The project uses:

Minimum SDK: 24
Target SDK: 36
Compile SDK: 36.1
Java: 21
Gradle: 9.5
⭐ Project Summary

SpendWise demonstrates the development of a modern Android personal finance application using Kotlin, Jetpack Compose, Room Database, ViewModel, Repository, Kotlin Coroutines, and Flow.

The application combines:

Modern Android UI
Local database persistence
CRUD operations
Income and expense tracking
Budget management
Transaction search and filtering
Spending analytics
Reactive UI updates
Profile and application settings
Expense report generation
Theme customization

The primary goal of SpendWise is to provide a simple, structured, and user-friendly solution for personal expense and budget management.
>>>>>>> origin/master
