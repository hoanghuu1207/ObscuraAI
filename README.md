# 🎬 BlurVision (Obscura AI)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="BlurVision Logo" />
</p>

<p align="center">
  <b>A modern, high-performance Android Video Blur & Auto Face Tracking application powered by Jetpack Compose, OpenGL ES 2.0, Google ML Kit, and Firebase Cloud Messaging.</b>
</p>

<p align="center">
  <a href="#-key-features"><img src="https://img.shields.io/badge/Android-Min%20SDK%2024-brightgreen.svg" alt="Android Min SDK 24" /></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.0.0-blue.svg" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg" alt="Jetpack Compose" /></a>
  <a href="https://developer.android.com/training/graphics/opengl"><img src="https://img.shields.io/badge/Graphics-OpenGL%20ES%202.0-orange.svg" alt="OpenGL ES 2.0" /></a>
  <a href="https://developers.google.com/ml-kit"><img src="https://img.shields.io/badge/AI-Google%20ML%20Kit-red.svg" alt="Google ML Kit" /></a>
  <a href="https://firebase.google.com/docs/cloud-messaging"><img src="https://img.shields.io/badge/Push-Firebase%20FCM-FFCA28.svg" alt="Firebase FCM" /></a>
  <a href="#-localization"><img src="https://img.shields.io/badge/Languages-10%2B%20Locales-purple.svg" alt="10+ Languages" /></a>
</p>

---

## Key Features

### Firebase Cloud Messaging (FCM) Push Notifications
- **Custom Messaging Service (`ObscuraFirebaseMessagingService`)**: Handles push notification payloads and data triggers in background & foreground states.
- **Android 13+ Runtime Permission**: Integrated `POST_NOTIFICATIONS` runtime permission launcher in `HomeScreen` to request notification access on Android 13 (API 33) and newer.
- **Notification Channels**: System notification channel (`obscura_fcm_channel`) with auto-cancel flags and `PendingIntent` launching `MainActivity`.

### AI Auto Face Detection & Lerp Tracking
- **On-Device Keyframe Scanning**: Asynchronously scans video frames using **Google ML Kit Face Detection** without sending media data to external servers.
- **Linear Interpolation (`lerp`)**: Computes smooth frame-by-frame face bounding box transitions (`getInterpolatedFaceRect`) between 250ms keyframe samples.
- **Spatial Distance Face Continuity**: Uses Euclidean distance matching to lock onto a single primary person's face across complex scenes with multiple people.
- **Dynamic Head Padding**: Automatically expands face bounding boxes by 15% to comfortably cover hair, chin, and fast movements.

### Shader-Based Video Blur Pipeline
- **Real-Time GLSL Shaders**: GPU-accelerated OpenGL ES 2.0 shaders supporting multiple blur styles.
- **Aspect Ratio Locking**: Supports `Freeform`, `1:1`, `4:3`, and `16:9` blur box constraints.
- **Interactive Drag & Resize Overlay**: Custom `DraggableFrameView` overlay for touch-based manual positioning.

### Hardware-Accelerated Export Engine
- **Direct GPU Encoding**: Renders video frames through OpenGL ES 2.0 onto an `EGLWindowSurface` bound to `MediaCodec`.
- **MPEG-4 Multiplexing**: Merges processed video tracks with original audio tracks using `MediaMuxer` and `MediaExtractor`.

### State History & Undo / Redo
- **Full History Stack**: ViewModel maintains `undoStack` and `redoStack` tracking every parameter change (intensity, blur style, frame shape, ratio).

### Localization & Multi-Language Support
Supports **10+ languages** out of the box with automatic system locale detection and manual override stored in **Jetpack DataStore**:
- 🇺🇸 English (`en`)
- 🇻🇳 Vietnamese (`vi`)
- 🇩🇪 German (`de`)
- 🇫🇷 French (`fr`)
- 🇮🇩 Indonesian (`in`)
- 🇯🇵 Japanese (`ja`)
- 🇰🇷 Korean (`ko`)
- 🇵🇹 Portuguese (`pt`)
- 🇪🇸 Spanish (`es`)
- 🇮🇳 Hindi (`hi`)
- 🇨🇳 Chinese (`zh`)

---

## Architecture & Tech Stack

The project strictly follows **Clean Architecture** principles with **MVI (Model-View-Intent)** state management and **Unidirectional Data Flow (UDF)**.

```
BlurVision/
├── app/src/main/java/com/techvertex/obscura/
│   ├── core/
│   │   ├── datastore/       # DataStore preferences
│   │   ├── notification/    # FirebaseMessagingService & FCM Channel
│   │   └── video/           # Core video processing engine
│   │       ├── export/      # MediaCodec, EGLWindowSurface & MediaMuxer pipeline
│   │       ├── face/        # ML Kit Face Scanner
│   │       ├── gl/          # OpenGL ES 2.0 Shaders & Texture Renderer
│   │       ├── model/       # Data models
│   │       ├── player/      # ExoPlayer Media3 SurfaceTexture wrapper
│   │       └── view/        # Custom GLSurfaceView & Draggable Frame View
│   ├── feature/
│   │   ├── blurvideo/       # Blur Video Editor Screen & ViewModel (MVI)
│   │   ├── home/            # Home Screen, Permission Launcher & Video Picker
│   │   ├── intro/           # Onboarding Page View Flow
│   │   ├── settings/        # Language & App Preferences Screen
│   │   └── splash/          # App Router & DataStore Entry Guard
│   ├── navigation/          # Jetpack Compose Navigation Graph
│   └── ui/theme/            # Material 3 Color Palette, Typography & Shapes
└── .github/workflows/       # Production CI/CD & Release Workflows
```

### Technical Stack & Dependencies

| Category | Technology / Library | Description |
| :--- | :--- | :--- |
| **Language** | [Kotlin 2.0](https://kotlinlang.org/) | Core language with Coroutines & Flow |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | 100% Declarative UI with Material 3 |
| **Architecture** | MVI / MVVM | Clean Architecture with StateFlow & UDF |
| **Dependency Injection** | [Hilt](https://dagger.dev/hilt/) | Compile-time dependency injection |
| **Video Playback** | [AndroidX Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3) | Frame decoding to SurfaceTexture |
| **Graphics & Shaders** | OpenGL ES 2.0 + EGL14 | Custom GLSL fragment & vertex shaders |
| **On-Device AI** | [Google ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection) | High-speed face scanning & tracking |
| **Push Notifications** | [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging) | Push notifications & token lifecycle |
| **Storage & State** | Jetpack DataStore Preferences | Reactive settings & state persistence |
| **Telemetry & Crash** | Firebase Analytics & Crashlytics | Real-time crash monitoring |
| **CI/CD** | GitHub Actions + Firebase App Distribution | Automated linting, testing & releases |

---

## Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Version 21
- **Android Device / Emulator**: Android 7.0 (API 24) or higher

## CI/CD & Automated Release Pipeline

This project features a complete **GitHub Actions CI/CD Pipeline** (`.github/workflows/android_ci_cd.yml`):

### 1. Continuous Integration (CI)
- Triggers on every `push` and `pull_request` to `main` or `master`.
- Executes `./gradlew lintDebug` and `./gradlew testDebugUnitTest`.
- Compiles `app-debug.apk` and uploads it to GitHub Actions **Artifacts**.
- Automatically deploys Debug builds to **Firebase App Distribution**.

### 2. Continuous Deployment (CD)
- Triggers when a release tag is pushed (`git tag v1.0.0` & `git push origin v1.0.0`).
- Decodes base64 Keystore from GitHub Secrets and signs the release binary.
- Compiles Signed Release APK & Android App Bundle (`.aab`).
- Automatically creates a **GitHub Release** with auto-generated changelog and uploads builds to **Firebase App Distribution**.
