# 1. Navigation, DI, and State Management Architecture

Date: 2026-03-31

## Status

Accepted

## Context

Porting `quran_android` to Kotlin Multiplatform (Android, Desktop, iOS) requires uniform cross-platform navigation, dependency injection, state management, and adaptive window rendering.

## Decision

1. **Navigation**: Adopt official `androidx.navigation:navigation-compose` Multiplatform with type-safe routing.
2. **Dependency Injection**: Adopt `Koin` (`io.insert-koin:koin-core` + `koin-compose` / `koin-compose-viewmodel`) for multiplatform dependency injection without code-gen overhead.
3. **State Management**: Use official `androidx.lifecycle:lifecycle-viewmodel-compose` Multiplatform with Unidirectional Data Flow (`StateFlow` UI state + intent/event sealed classes).
4. **Adaptive Layouts**: Use `WindowSizeClass` Material 3 breakpoints in shared Compose UI to dynamically toggle Single Page (Compact/Medium) vs Dual Page (Expanded desktop/tablet) Mushaf reader layouts.

## Consequences

- Direct architectural parity with modern Android Jetpack Compose code.
- Zero platform-specific UI forks for tablet/desktop vs mobile.
- Clean Koin module separation per feature/layer (`coreModule`, `audioModule`, `databaseModule`, `readerModule`).
