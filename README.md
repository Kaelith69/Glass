# CineCommon

<p align="center">
  <img src="docs/readme/banner.svg" alt="CineCommon banner" width="100%" />
</p>

CineCommon is a local-first Android movie app built with Jetpack Compose, Room, and StateFlow.

## Overview

The app currently focuses on a single, cohesive experience:

- onboarding with a unique username claim
- discovery, search, and genre filtering
- movie detail views with reviews, watchlist, and wishlist actions
- community lists, badges, notifications, and trust-based moderation

## Architecture

<p align="center">
  <img src="docs/readme/architecture.svg" alt="Architecture diagram" width="100%" />
</p>

The app uses a single-activity Compose setup:

- `MainActivity` hosts the Compose tree
- `CineCommonApp` handles screen routing
- `MainViewModel` owns UI state and actions
- `AppRepository` applies business rules and persistence
- `Room` stores users, movies, reviews, lists, badges, notifications, and edit history

## Data flow

<p align="center">
  <img src="docs/readme/data-flow.svg" alt="Data flow diagram" width="100%" />
</p>

State moves in one direction:

1. UI events enter the ViewModel
2. the repository validates and writes to Room
3. Room changes are exposed back through `StateFlow`
4. Compose recomposes from the latest state

## Folder structure

<p align="center">
  <img src="docs/readme/folder-structure.svg" alt="Folder structure diagram" width="100%" />
</p>

## Theme system

<p align="center">
  <img src="docs/readme/theme-system.svg" alt="Theme system diagram" width="100%" />
</p>

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- Coroutines and Flow
- KSP
- Roborazzi

## Prerequisites

- Android Studio
- Android SDK 36
- A local Gradle installation

## Build and test

The repository does not include a Gradle wrapper. Use a local Gradle install compatible with the Android Gradle Plugin version in `gradle/libs.versions.toml`.

- Build: `gradle assembleDebug`
- Test: `gradle test`

## Configuration

Release signing expects these environment variables:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

The repository also includes `.env.example` for the Secrets Gradle Plugin.

## License

This project is released under the MIT License. See [LICENSE](LICENSE).
