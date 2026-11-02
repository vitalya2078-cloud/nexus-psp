# NEXUS PSP — Android

Early Android project scaffold for a custom PSP emulator frontend.

## Current state
- Kotlin + Jetpack Compose app shell
- Home, Library and Settings tabs
- Dark custom visual theme
- Native C++ / JNI placeholder
- CMake configuration

## Not implemented yet
- PPSSPP core integration
- ISO/CSO scanning and import
- Save states
- Touch controls and controller support
- Actual game execution

## Build requirements
- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools
- Android NDK
- CMake 3.22.1
- Gradle 8.9

Open the project root in an Android Gradle-compatible IDE. For native builds, install the matching NDK and CMake components.

## License note
This scaffold is original project code. When integrating PPSSPP, comply with its GPL-2.0-or-later license and preserve required notices/source availability for distributed derivative builds.

## Cloud APK build (GitHub Actions)

This repository includes `.github/workflows/android.yml`.
Open **Actions → Build NEXUS PSP APK → Run workflow**.
When the workflow finishes, download the `nexus-psp-debug-apk` artifact.

The workflow installs JDK 17, Android SDK Platform 35, NDK and CMake, then runs `:app:assembleDebug`.
This is a debug/test APK, not a signed Play Store release.
