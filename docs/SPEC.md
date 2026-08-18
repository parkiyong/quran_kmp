# Specification: Port `quran_android` to Kotlin Multiplatform (Android, Desktop, iOS)

## Problem Statement

Users of `quran_android` on Android enjoy a rich, reliable Quran reading and audio listening experience with page-accurate Mushafs, verse highlighting, multiple translations, and reciters. However, desktop users (macOS, Windows, Linux) and iOS users do not have access to this unified open-source ecosystem, and maintaining fragmented platform-specific apps leads to inconsistent features, divergent schemas, and maintenance overhead.

## Solution

A single, modern Kotlin Multiplatform (Compose Multiplatform) application supporting Android, Desktop (JVM), and iOS with full UI/UX and feature parity for core reader, audio recitation, translations, search, bookmarks, and downloads, while maintaining 100% compatibility with existing Quran datasets, audio servers, and SQLite database schemas.

## User Stories

1. As a Quran reader on any supported platform (Android, iOS, Desktop), I want to view full-page Mushafs (Madani, IndoPak, Tajweed) rendered at high resolution, so that I can read the Quran with authentic typography and layout.
2. As a reader, I want the page layout to automatically adapt between single-page view on mobile/narrow windows and dual-page view on tablets/expanded desktop windows, so that screen space is utilized effectively in RTL reading order.
3. As a reader, I want to tap on any Ayah on the Mushaf page, so that the verse is highlighted and contextual actions (play audio, copy, bookmark, view translation/tafsir) appear.
4. As a reader, I want to swipe horizontally to turn pages with realistic page transitions, so that the reading flow feels natural.
5. As a reader, I want to toggle Night Mode, so that page colors invert comfortably for low-light reading while preserving highlight visibility.
6. As a listener, I want to stream or download audio recitations from multiple Qaris, so that I can listen to my preferred reciter online or offline.
7. As a listener, I want to listen to gapless full-surah recitations with real-time Ayah and word highlighting, so that I can follow along seamlessly as each verse is recited.
8. As a listener, I want to configure audio repeats (per-verse repeat count, surah range repeat count), so that I can memorize and review verses systematically.
9. As a listener, I want audio playback to continue seamlessly in the background with lock screen and notification media controls on Android and iOS, so that I can control playback without keeping the app open.
10. As a reader, I want to download translation and tafsir databases in multiple languages, so that I can study verse meanings offline.
11. As a reader, I want to view side-by-side or sliding drawer translation/tafsir overlays for any selected Ayah, so that I can understand the meaning without losing my reading position.
12. As a student, I want to bookmark specific Ayahs and pages with custom color-coded tags, so that I can organize my revision and study topics.
13. As a user, I want the app to automatically save my last-read page per Mushaf type, so that I can resume reading instantly upon opening the app.
14. As a user, I want to search Quran text in Arabic (with or without diacritics) and across downloaded translations, so that I can quickly locate specific verses.
15. As a user on limited network bandwidth, I want downloads of Mushaf image packs, audio files, and databases to support byte-range pausing and automatic resumption, so that interrupted downloads do not restart from zero.
16. As a user, I want downloaded asset packs and database archives to be automatically verified for integrity via streaming checksums, so that corrupt files are never mounted.
17. As a user, I want to navigate between Surah index, Juz' index, Bookmarks, and Settings via a unified navigation bar/drawer, so that navigation is predictable across desktop and mobile.
18. As a desktop user, I want keyboard shortcuts (arrow keys for page turning, space for play/pause, / for search), so that reading is productive without touch gestures.
19. As a user, I want to configure custom font sizes for translations and Arabic typography options in Settings, so that reading comfort is personalized.
20. As a user, I want all core app preferences and download caches to persist reliably across app restarts, so that my configuration is preserved.

## Implementation Decisions

1. **Shared Multiplatform Architecture**:
   - Built on Kotlin Multiplatform targeting Android (`androidMain`), iOS (`iosMain`), and Desktop JVM (`jvmMain`).
   - Shared UI implemented using Jetpack Compose Multiplatform.
   - Core dependency injection driven by Koin Multiplatform.

2. **Navigation & State Flow**:
   - Screen routing and deep-linking handled uniformly via AndroidX Navigation Compose Multiplatform with type-safe route objects.
   - UI state managed using AndroidX Lifecycle ViewModel Multiplatform with Unidirectional Data Flow (`StateFlow` UI state + sealed event intents).
   - Adaptive responsive layouts powered by Material 3 `WindowSizeClass` breakpoints (Compact/Medium = Single Page RTL; Expanded = Dual Page RTL).

3. **Database & Data Layer**:
   - Dynamic user data (bookmarks, tags, history, download metadata) managed via SQLDelight 2.x with platform SQLite drivers (`AndroidSqliteDriver`, `NativeSqliteDriver`, `JdbcSqliteDriver`).
   - Reused legacy database schemas directly from `quran_android` (`Bookmark.sq`, `Tag.sq`, `AyahGlyphs.sq`, `AyahMarker.sq`, `translations.sq`, `WordAlignment.sq`).
   - Dynamic and downloaded SQLite databases (`ayahinfo_*.db`, `quran.ar.db`, `quran.<lang>.db`, `<sura>_<qari>.db`) queried via a platform-agnostic SQLite driver connection abstraction.
   - Cross-platform filesystem paths and file I/O managed strictly via Okio `FileSystem` and `Path`.

4. **Audio Engine Architecture**:
   - Unified `AudioQueue` state machine and `TimingRepository` located in `commonMain` handling verse advancement, Basmalah injection, and repeat loops.
   - Platform audio player implementations:
     - Android: AndroidX Media3 ExoPlayer + Foreground Playback Service + MediaSession.
     - iOS: Native `AVPlayer` + CMTime boundary observers + `MPNowPlayingInfoCenter` + `MPRemoteCommandCenter`.
     - Desktop JVM: Native audio player engine wrapper with millisecond polling callbacks.

5. **Page Rendering & Geometry**:
   - Mushaf page images loaded asynchronously and rendered to Compose Multiplatform `Canvas` using source-to-canvas coordinate transformation matrices.
   - Over-image Ayah highlights drawn with `BlendMode.Multiply` for crisp readability; background tinting / clipping executed via `BlendMode.SrcOver`.
   - Touch detection uses inverse matrix transformation from canvas touch points to image coordinate bounds.

6. **File Downloader & Asset Engine**:
   - Unified `DownloadEngine` in `commonMain` built on Ktor HTTP Client supporting HTTP `Range: bytes=X-` resumption (HTTP 206/200/416) and mirror fallback URLs.
   - Streaming hash calculation (MD5/SHA256) via Okio `HashingSink` during transfer to eliminate post-download disk re-reads.
   - Multiplatform safe zip decompression with Zip-Slip protection and path sanitization.
   - Platform background hosting: Android Foreground Service + WorkManager, iOS `beginBackgroundTask`, Desktop coroutine background workers.

## Testing Decisions

- **Testing Philosophy**: Test purely external behavior via observable state outputs and side effects; avoid coupling tests to private implementation details or internal state transitions.
- **Modules Tested**:
  - **ViewModels & Presenters**: Tested via Coroutines `runTest` and Turbine, asserting on emitted `UiState` sequences in response to user intents.
  - **AudioQueue & Timing Engine**: Tested against gapless and gapped timing tables, verifying accurate verse advancement, repeat count enforcement, and boundary emissions.
  - **DownloadEngine**: Tested using mock Ktor HTTP engines simulating network dropouts, 206 Partial Content resumption, checksum mismatches, and Okio in-memory `FileSystem` decompression.
  - **Page Coordinate & Geometry Engine**: Tested with boundary coordinates asserting correct Ayah detection on hit testing and matrix conversions across diverse viewport aspect ratios.
  - **SQLDelight Repositories**: Tested using in-memory SQLite driver across all schema queries and migrations.
- **Prior Art**: Parity with unit test suites in `quran_android` (`AudioQueueTest`, `TimingRepositoryTest`, `SearcherTest`, `PageGlyphsCoordsTest`).

## Out of Scope

- Android Auto (`autoquran`), Wear OS / Watch companions, and Android home screen widgets (deferred to future platform-specific extension modules).
- Rewriting or altering remote database schemas, CDN endpoints, or audio server protocols (existing `quran_android` infrastructure compatibility is preserved 1:1).

## Further Notes

- Full research reports and architectural decision records are archived in `docs/research/` and `docs/adr/` (ADR 0001 through 0005).
- Reference legacy implementation: `C:\Users\wraja\GitHub\quran_android`.
