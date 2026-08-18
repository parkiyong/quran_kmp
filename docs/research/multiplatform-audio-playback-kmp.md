# Research: Multiplatform Audio Playback & Ayah Highlighting Architecture

Investigates issue #3: Multiplatform audio playback architecture for streaming/local audio recitation with millisecond-accurate Ayah and word boundary timing synchronization across Android, iOS, and Desktop (JVM).

---

## 1. Domain Audio Architecture & Legacy Inventory

Analysis of `C:/Users/wraja/GitHub/quran_android` (`AudioService.kt`, `AudioQueue.kt`, `SuraTimingDatabaseHandler.kt`, `TimingRepository.kt`):

### Two Recitation Formats
1. **Gapless Recitations** (1 file per Surah, e.g., `002.mp3` or `002.opus`):
   - Continuous audio stream containing the full Surah recitation.
   - Synchronized via SQLite timing databases (`<qari_db>.db`, table `timings`: `sura`, `ayah`, `time` in milliseconds, `words` `ayah:start:end,...`).
   - Requires dynamic seeking for Ayah navigation and millisecond-accurate position tracking to trigger highlight updates.
   - Special timing entries: ayah `0` (Basmalah intro), ayah `999` (End-of-sura marker).

2. **Gapped Recitations** (1 file per Ayah, e.g., `001001.mp3`, `001002.mp3`):
   - Discrete audio files per verse.
   - Ayah change occurs on file boundary / track transition.
   - Basmalah / Isti'atha audio is prepended when transitioning to new Surahs (except Surah 9 At-Tawbah).

---

## 2. Platform Audio Engine Evaluation

| Dimension | Android | iOS | Desktop (JVM) |
| :--- | :--- | :--- | :--- |
| **Primary Native API** | AndroidX Media3 ExoPlayer (`androidx.media3:media3-exoplayer`) | AVFoundation (`AVPlayer`, `AVPlayerItem`, `AVAudioSession`) | JVM Audio (JavaFX `MediaPlayer` or Miniaudio / VLCJ / JLayer) |
| **Millisecond Position Accuracy** | Sub-millisecond (`player.currentPosition`) | Nanosecond/CMTime via `AVPlayer.addBoundaryTimeObserver` or periodic polling | Millisecond via media timeline callbacks / polling |
| **Streaming & Disk Caching** | `CacheDataSource.Factory` + `SimpleCache` (Media3 LRU) | Native `AVURLAsset` HTTP Live Streaming cache or custom `AVAssetResourceLoaderDelegate` | Ktor/Okio disk cache + local file playback |
| **Background / Lockscreen Controls** | Android `MediaSession` + Foreground `Service` with `MediaStyle` notification | `MPNowPlayingInfoCenter` + `MPRemoteCommandCenter` (`UIBackgroundModes: audio`) | System media keys / desktop tray (optional) |
| **Opus Codec Support** | Native Android `MediaCodecAudioRenderer` | Supported in iOS 11+ via CoreMedia/AVFoundation container (`.caf` / `.m4a` / raw Ogg Opus via cinterop/libopus) | Requires libopus or standard MP3 stream |

### Platform Recommendations

### 1. Android Target (`androidMain`)
- **Engine**: **AndroidX Media3 ExoPlayer** (`androidx.media3:media3-exoplayer`, `media3-session`).
- **Foreground Service**: Retain `AudioService` as an Android Service to comply with Android 14+ foreground service types (`foregroundServiceType="mediaPlayback"`).
- **Audio Focus**: Managed via `AudioAttributes.Builder().setContentType(AUDIO_CONTENT_TYPE_SPEECH).setUsage(USAGE_MEDIA).build()` and `player.setAudioAttributes(..., true)`.
- **Caching**: Media3 `SimpleCache` with 128MB LRU and revision-aware cache keys (`$url|audio_revision=$rev`).

### 2. iOS Target (`iosMain`)
- **Engine**: **AVFoundation** (`AVPlayer`, `AVPlayerItem`).
- **Timing Synchronization**:
  - `AVPlayer.addPeriodicTimeObserver(forInterval:queue:using:)` with `CMTimeMake(value = 50, timescale = 1000)` (50ms interval) or boundary time observers `addBoundaryTimeObserver(forTimes:queue:using:)` generated directly from the Ayah/word timing list.
- **Background Playback**:
  - Enable `UIBackgroundModes` with `audio` in `Info.plist`.
  - Set `AVAudioSessionCategoryPlayback` on `AVAudioSession.sharedInstance()`.
  - Update `MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo` on Ayah change.
  - Register callbacks on `MPRemoteCommandCenter.sharedCommandCenter()` for `playCommand`, `pauseCommand`, `nextTrackCommand`, `previousTrackCommand`.

### 3. Desktop Target (`jvmMain`)
- **Engine Options**:
  - **Option A (Recommended for KMP Desktop)**: **JavaFX / OpenJFX Media** (`javafx.scene.media.MediaPlayer`) or **KMP Native Wrapper around Miniaudio / MP3 Decoder**.
  - JavaFX `MediaPlayer` natively handles HTTP streaming, local files, pause/play, seek, and accurate current time observation via `currentTimeProperty()`.
  - Alternative lightweight: Ktor HTTP streaming + local file download to cache + pure Kotlin/Java MP3 stream player (e.g. JLayer/BasicPlayer) for local files.

---

## 3. Unified Cross-Platform Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        commonMain (Core Logic)                         │
├────────────────────────────────────────────────────────────────────────┤
│  • AudioQueue (Ayah bounds, repeat rules, basmalah injection)         │
│  • TimingRepository (SQLDelight timings.db query, LRU cache)           │
│  • AudioPlayerStateFlow (AudioStatus.Playback, AudioStatus.Stopped)    │
│  • Expect/Actual: AudioPlayerPlatformBridge                            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│ androidMain  │             │   iosMain    │             │   jvmMain    │
├──────────────┤             ├──────────────┤             ├──────────────┤
│ • Media3     │             │ • AVPlayer   │             │ • JavaFX/    │
│   ExoPlayer  │             │ • CoreMedia  │             │   Miniaudio  │
│ • Service &  │             │ • MPNowPlay- │             │ • Local Cache│
│   MediaSession│            │   ingInfo    │             │   Player     │
└──────────────┘             └──────────────┘             └──────────────┘
```

### Core Abstractions in `commonMain`

#### 1. Audio Player Interface
```kotlin
interface AudioPlayer {
    val status: StateFlow<AudioStatus>
    fun play(request: AudioRequest)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
}
```

#### 2. Ayah & Word Timing Data Model
```kotlin
data class SuraTimings(
    val ayahTimings: Map<Int, Long>,           // Ayah -> start ms
    val wordTimings: Map<Int, List<WordTiming>> // Ayah -> List of word timings
)

data class WordTiming(
    val word: Int,
    val startTime: Long,
    val endTime: Long
)
```

#### 3. Pure Kotlin Audio Queue (`AudioQueue.kt`)
Port the legacy Android `AudioQueue` logic to pure Kotlin in `commonMain`:
- Tracks current `sura` and `ayah`.
- Handles repeat counters (`timesPlayed`, `rangePlayedTimes`).
- Computes next/previous ayah navigation respecting Surah boundaries (1..114) and Ayah counts.
- Resolves Basmalah/Isti'atha insertion state for gapped/gapless reciters.

---

## 4. Ayah & Word Boundary Synchronization Strategy

For Gapless Recitation, timing synchronization follows a **hybrid boundary-scheduling and polling algorithm**:

1. **Database Timing Retrieval**:
   - Query `timings.db` via SQLDelight when a Surah begins.
   - Construct binary-searchable array of `(ayah, startMs)` and list of `(wordIndex, startMs, endMs)`.

2. **Accurate Highlight Scheduling**:
   - Instead of fixed 16ms high-frequency polling (which drains battery), use **adaptive delay scheduling**:
     $$\text{delayMs} = \frac{\text{nextTargetTimeMs} - \text{currentPositionMs}}{\text{playbackSpeed}}$$
     clamped to $[50\text{ms}, 1000\text{ms}]$.
   - When near boundary ($<150\text{ms}$), clamp delay to $25\text{ms}$ for smooth visual transition.

3. **Platform Dispatch**:
   - **Android**: `Handler` delayed messages or Coroutine delay loop in the playback service.
   - **iOS**: `AVPlayer.addBoundaryTimeObserver` for verse boundaries + `addPeriodicTimeObserver` for word highlighting.
   - **Desktop**: Coroutine timer loop observing `player.currentPosition`.

4. **UI State Emission**:
   - State updates emit to `AudioStatusRepository.audioStatus: StateFlow<AudioStatus>`.
   - Compose Multiplatform UI collects `audioStatus` and highlights the matching Ayah bounding box / word token in the Mushaf canvas.

---

## 5. Offline Storage & Caching Strategy

1. **Gapped Recitations**:
   - Download individual Ayah MP3s into `<app_data>/audio/<qari_id>/<sura>_<ayah>.mp3`.
   - Player plays directly from local file URL if file exists, falls back to remote CDN URL.

2. **Gapless Recitations**:
   - Download complete Surah MP3/Opus file into `<app_data>/audio/<qari_id>/<sura>.<ext>`.
   - Accompanying `<qari_id>.db` timing file downloaded into `<app_data>/audio/databases/`.
   - Streaming cache uses HTTP chunk caching on Android (Media3 `CacheDataSource`) and local temp files on iOS/Desktop.

---

## Primary Sources & References
- Legacy `quran_android` Playback & Timing Engine:
  - `app/src/main/java/com/quran/labs/androidquran/service/AudioService.kt`
  - `app/src/main/java/com/quran/labs/androidquran/presenter/audio/service/AudioQueue.kt`
  - `app/src/main/java/com/quran/labs/androidquran/database/SuraTimingDatabaseHandler.kt`
  - `app/src/main/java/com/quran/labs/androidquran/data/TimingRepository.kt`
  - `common/audio/src/main/java/com/quran/labs/androidquran/common/audio/model/playback/AudioStatus.kt`
  - `common/audio/src/main/java/com/quran/labs/androidquran/common/audio/model/playback/AudioRequest.kt`
- Official Platform Audio Specs:
  - AndroidX Media3 ExoPlayer Architecture: https://developer.android.com/media/media3/exoplayer
  - Apple AVFoundation `AVPlayer` & Boundary Time Observers: https://developer.apple.com/documentation/avfoundation/avplayer
  - Apple MediaPlayer `MPNowPlayingInfoCenter`: https://developer.apple.com/documentation/mediaplayer/mpnowplayinginfocenter
