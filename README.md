# Mellune

### A Material 3 Expressive (M3E) fork of Lawnchair

[![Build & Release Mellune](https://github.com/shreyagarwal72/mellune/actions/workflows/build_release_apk.yml/badge.svg)](https://github.com/shreyagarwal72/mellune/actions/workflows/build_release_apk.yml)
[![GitHub Releases](https://img.shields.io/github/v/release/shreyagarwal72/mellune?include_prereleases&label=Release&logo=github)](https://github.com/shreyagarwal72/mellune/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/shreyagarwal72/mellune/total.svg?label=Downloads&logo=github)](https://github.com/shreyagarwal72/mellune/releases)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)

<picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/device-frame-dark.webp" width="250px">
    <img alt="Mellune home screen featuring Material 3 Expressive dynamic color theming, clock widget, and themed icons." src="docs/assets/device-frame-light.webp" width="250px">
</picture>

**Mellune** is a free, open-source **Material 3 Expressive (M3E) fork of [Lawnchair](https://github.com/LawnchairLauncher/lawnchair)**, built on Android 16/17 AOSP **Launcher3**. It combines Lawnchair's customizable foundation with expressive Material 3 UI components, physics-driven multi-layer animations, Pixel Launcher 17-inspired search and organization utilities, and QuickSwitch recents integration.

---

## Key Features

### Material 3 Expressive (M3E) Design System
- **Expressive UI Controls**: Custom rolling-thumb Canvas slider (`MelluneSlider`), shape-morphing `MelluneSegmentedButtonGroup`, tonal `SegmentedListItem` preference rows, and an expressive About hero & highlights screen.
- **Dynamic Monet & Icon Styling**: Wallpaper-derived color extraction (`AccentColorExtractor`), monochrome themed icon palette loader, icon shape customization, and localized UI across supported languages.

### Physics & Unlock Animations (`MelluneSpringEngine`)
- **Three-Layer Motion Engine**: Spring-driven foreground (launched icon / hotseat), midground (workspace page, icons, and widgets), and background (wallpaper zoom + API 31+ `RenderEffect` blur) transitions for app launch, close/return, and screen unlock.
- **Ring-Appear Unlock**: Concentric ring stagger across workspace items on device unlock.
- **AGSL Unlock Shimmer Overlay**: Optional GPU `RuntimeShader` turbulence-noise shimmer sweeping across the workspace on unlock (toggleable in Home screen settings).

### Unified App Drawer Search & Quick Settings
- **Type-Ahead Action Chip**: Inline `"open"` chip (`AllAppsSearchInput`) for immediate top-match launching.
- **Actionable Quick Settings in Search**: Live toggle tiles and control shortcuts in drawer search via `QuickSettingsSearchProvider` (Flashlight, Internet & Wi-Fi, Bluetooth, Sound & Volume, Dark theme, Night Light, Airplane mode, Hotspot & Tethering).
- **Keyboard Pin Prompt**: Non-intrusive inline `"Always show keyboard here?"` snackbar (`PinKeyboardSnackBar`) in the app drawer.
- **Flexible Web & Device Search**: Local app/shortcut/contact matching plus configurable web search providers including SearXNG.

### Workspace Organizer, Curated Spaces & Home Screen Files
- **Workspace Organizer**: Dedicated page manager (`WorkspaceOrganizerActivity`) to reorder, add blank pages, delete empty pages, or switch the default home page.
- **Curated Spaces**: `SpaceCreatorActivity` groups your most-used or selected apps into a topic-based workspace page or folder in one tap.
- **Home Screen Files Sync**: `HomeScreenFilesManager` pins files placed in `Downloads/Home screen` straight into a dedicated `"Home screen files"` folder on your workspace.
- **Fixed Landscape Layout**: Dedicated `8×3` phone landscape grid toggle (`fixed_landscape_mode`).

### Widgets, Smartspace & QuickSwitch
- **Expressive Widget Resize Frame**: Pill-shaped Material 3 corner grab handles on widget resize outlines.
- **At a Glance & Smartspacer**: Native Smartspace support with integration for [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
- **QuickSwitch Support**: Recents provider integration for Android 15–16 (`minSdk 26`, `targetSdk 37`, Quickstep SDK 35–36; root required for system recents integration).

---

## Download

<p align="left">
  <a href="https://github.com/shreyagarwal72/mellune/releases">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="docs/assets/badge-github.webp" height="60">
      <img alt="Get it on GitHub" src="docs/assets/badge-github.webp" height="60">
    </picture>
  </a>
</p>

- **GitHub Releases & CI Artifacts**: Grab the universal signed release APK (`Mellune-Release.apk`) from [Releases](https://github.com/shreyagarwal72/mellune/releases) or the latest [`Build & Release Mellune`](https://github.com/shreyagarwal72/mellune/actions/workflows/build_release_apk.yml) workflow run artifacts.
- **Package ID**: `com.mellune.launcher` (release/github) · `com.mellune.launcher.debug` (debug)

---

## Building from Source

Requires **JDK 21**, Android SDK (`compileSdk 37` / platform extensions), and initialized git submodules:

```bash
git clone --recursive https://github.com/shreyagarwal72/mellune.git
cd mellune
./gradlew assembleLawnWithQuickstepGithubRelease
```

---

## Upstream Attribution & License

Mellune is an independent fork derived from [**Lawnchair**](https://github.com/LawnchairLauncher/lawnchair) and AOSP **Launcher3**. Huge thanks to the entire Lawnchair team and contributors for making the upstream project possible.

Licensed under the [Apache License 2.0](LICENSE.txt). See [CONTRIBUTING.md](CONTRIBUTING.md) if you would like to contribute.
