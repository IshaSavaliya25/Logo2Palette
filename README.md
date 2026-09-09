# 🎨 Logo2Palette

> **Transform Brand Logos into Production-Ready, WCAG-Compliant UI Color Systems & Design Tokens.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue?style=flat-square)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-orange?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](#license)

---

## 📌 Overview

**Logo2Palette** is an intelligent Android application designed for designers, mobile engineers, and frontend developers. By uploading any brand or company logo, the app algorithmically extracts dominant and accent tones, computes harmonious UI color roles (Primary, Secondary, Accent, Background, Surface, and Text), validates accessibility according to **WCAG 2.1 standards**, and exports production-ready code tokens for modern web and mobile frameworks.

---

## ✨ Key Features

### 🖼️ Intelligent Color Extraction & Harmonization
- **Hybrid Sampling**: Integrates the AndroidX Palette API with deep pixel-grid scanning to extract both broad gradients and crisp vector accents.
- **Smart Filtering**: Eliminates canvas whites, border shadows, and duplicates using Euclidean color-distance math.
- **Dynamic Variations**: Generates multiple distinct, complementary palette variations from a single logo with a single tap.

### ♿ Real-Time WCAG 2.1 Accessibility
- Computes relative luminance and contrast ratios across all swatches on the fly.
- Displays visual compliance badges (**AAA**, **AA**, **AA Large**, or **FAIL**).
- Automatically selects contrasting typography colors for light and dark backgrounds.

### 📱 Live Web & Mobile UI Previews
- Instant interactive preview renders how the generated palette looks on modern landing pages and mobile interfaces.
- **Light & Dark Mode Switcher**: Inspect derived dark-mode tokens directly in real time.
- Responsive toggle between desktop webpage and mobile app layouts.

### 📄 Brand Identity PDF Export
- Generates a polished, multi-page vector **Brand Style Guide PDF** on device using Android's native `PdfDocument` engine.
- Includes embedded logo, color swatches, contrast metrics, code token tables, and live UI mockups.
- Share directly via Android's native share sheet (`FileProvider`).

### 💻 Multi-Platform Code Token Export
Export production-ready color code with one-tap copy:
- **CSS Variables** (`:root { ... }`)
- **Tailwind CSS** (`tailwind.config.js`)
- **Android XML** (`colors.xml`)
- **Jetpack Compose** (`Color.kt`)
- **Flutter** (`AppColors.dart`)
- **JSON Tokens**

### 👤 Profile & Palette History
- Cloud sync and offline storage for saved palettes.
- Firebase Authentication and Cloud Firestore integration with local session fallback.
- Revisit, preview, or re-export past palettes anytime.

---

## 🏗️ Architecture & Tech Stack

```
com.example.logo2palette
├── model
│   ├── ColorPalette.kt          # Complete color system & dark theme tokens
│   ├── SavedPalette.kt          # History model with timestamps & logo snapshot
│   └── User.kt                  # Account profile model
├── utils
│   ├── ColorExtractor.kt        # Sampling, deduplication & palette variation engine
│   ├── ColorUtils.kt            # Luminance, WCAG contrast calculation & code exporters
│   ├── PaletteHistoryManager.kt # Firestore / Local persistence
│   └── UserSessionManager.kt    # Auth state & user session handling
├── MainActivity.kt              # Workspace: upload, inspect, switch variations, actions
├── PreviewActivity.kt           # Interactive web & mobile app simulation
├── ProfileActivity.kt           # User profile & saved palettes gallery
├── PdfReportGenerator.kt        # Multi-page brand guide PDF renderer
├── LoginActivity.kt             # User login screen
└── RegisterActivity.kt          # User registration screen
```

### 🛠️ Built With
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: Android Views, Material Design 3, `ConstraintLayout`, `CardView`
- **Color Processing**: `androidx.palette:palette-ktx`
- **Concurrency**: Kotlin Coroutines (`Dispatchers.Default`, `lifecycleScope`)
- **Backend / Auth**: Firebase Authentication & Cloud Firestore (Firebase BoM)
- **Document Generation**: Android Native `PdfDocument` & `FileProvider`

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or newer
- **JDK**: Java 11 or higher
- **Android Device / Emulator**: Running Android 7.0 (API 24) or above

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/IshaSavaliya25/Logo2Palette.git
   cd Logo2Palette
   ```

2. **Add Firebase Configuration:**
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Download your `google-services.json` file.
   - Place `google-services.json` into the `app/` folder:
     ```
     Logo2Palette/
     └── app/
         └── google-services.json
     ```

3. **Build and Run:**
   - Open the project in Android Studio.
   - Let Gradle sync all dependencies.
   - Select an emulator or physical device and click **Run** (`Shift + F10`).

---

## 📖 How It Works

1. **Upload**: Select any logo (PNG, JPG, WebP) from device storage.
2. **Generate**: The color extraction engine samples dominant hues, checks contrast levels, and creates a full UI palette.
3. **Explore**: Press **"Another Palette"** to cycle through creative variations (vibrant, complementary, monochromatic, accented).
4. **Preview**: Tap **"Website & App Preview"** to see live rendered components in both light and dark themes.
5. **Export**: 
   - Tap **"Export Code"** to grab tokens for CSS, Tailwind, Compose, Flutter, or XML.
   - Tap **"PDF Report"** to generate and share a complete Brand Identity Guide.

