# 3. Multiplatform Audio Playback & Timing Synchronization

Date: 2026-03-31

## Status

Accepted

## Context

Quran recitation requires seamless gapless (full Surah) and gapped (verse-by-verse) audio playback with sub-50ms synchronization of active Ayah and word boundaries across Android, iOS, and Desktop.

## Decision

1. **Core State & Queue in `commonMain`**:
   - `AudioQueue` (repeat rules, ayah boundary advancement, basmalah/isti'atha injection).
   - `TimingRepository` querying SQLDelight timing database (`sura_timing.db`).
   - `AudioPlayer` interface exposing `StateFlow<AudioStatus>`.

2. **Platform Audio Engines**:
   - **Android**: AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-session`) with foreground playback service and MediaSession.
   - **iOS**: Native `AVPlayer` with boundary / periodic CMTime observers, `MPNowPlayingInfoCenter`, and `MPRemoteCommandCenter`.
   - **Desktop (JVM)**: JavaFX / Miniaudio audio engine wrapper with millisecond polling observer.

## Consequences

- Full gapless and gapped recitation support identical to legacy `quran_android`.
- Native lock-screen / notification controls on Android and iOS.
- Strict timing synchronization decoupled from platform UI code.
