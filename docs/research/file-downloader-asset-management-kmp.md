# Research: Multiplatform File Downloader & Asset Management in KMP

Investigates issue #5: Architecture and engine choice for downloading audio recitations, mushaf page packs, translations, and database assets with HTTP byte-range resume, checksum hashing, safe unzip decompression, and background lifecycle support across Android, iOS, and Desktop (JVM).

---

## 1. Domain Workloads & Legacy Inventory

Analysis of legacy implementation in `C:/Users/wraja/GitHub/quran_android` (`QuranDownloadService.kt`, `Downloader.kt`, `QuranFileUtils.kt`, `ZipUtils.java`, `MD5Calculator.kt`, `AudioUpdateWorker.kt`):

### Download Types and Asset Characteristics

| Asset Category | Target Formats & Typical Sizes | Download Pattern & Volume | Unzip / Post-Processing | Hashing / Integrity Check |
| :--- | :--- | :--- | :--- | :--- |
| **Mushaf Page Packs** | `images<width>.zip` (50MB–150MB), patch zips `images<width>_v<ver>.zip` (1MB–15MB) | Single large file per mushaf edition / density | Unpack ~604 PNG files to `width<widthParam>/page<NNN>.png` | File size check, zip structure validation |
| **Ayah Coordinates** | `ayahinfo<width>.zip` (1MB–5MB) | Single zip per screen density | Unpack `ayahinfo_<width>.db` to ayahinfo directory | Zip validation, SQLite integrity pragma |
| **Gapless Audio** | Sura audio files (`001.mp3`..`114.mp3`, 1MB–30MB each), timing db `<qari_db>.db.zip` | Batch per sura range (1 to 114 files) + 1 database file | Unzip timing database (`.db`), audio files saved directly | MD5 checksum verification against server update manifest |
| **Gapped Audio** | Ayah audio files (`001001.mp3`..`114006.mp3`, 20KB–300KB each) | High file-count batch per surah / page (up to 6,236 files) | Direct file writes to `<qari_path>/<sura>/<ayah>.mp3` | MD5 checksum verification |
| **Translations & Tafsir** | `quran.<lang>.<id>.db` or `.zip` (500KB–15MB) | Single file on user demand from translation catalog | Optional zip unpack; registered into `translations.db` | Schema version check, SQLite file verification |
| **Arabic Search DB** | `quran.ar.uthmani.v2.db.zip` (~2MB) | Single zip download on first search access | Unpack to databases directory | Zip validation |

---

## 2. Engine Comparison: Ktor Client vs Platform-Native Download Managers

### Engine Comparison Matrix

| Dimension | Option A: Pure Ktor + Okio Engine (Shared Core) | Option B: Platform-Native Downloaders (Android `DownloadManager`, iOS `NSURLSessionDownloadTask`) | Option C: Hybrid Architecture (Shared Ktor Engine + Platform Lifecycle Host) |
| :--- | :--- | :--- | :--- |
| **Code Sharing** | **~95%** in `commonMain` | **<20%** (3 disparate implementations) | **~90%** (Shared engine, thin platform service wrappers) |
| **Byte-Range Resume (`Range: bytes=X-`)** | Explicit, deterministic HTTP header handling via Ktor | System-dependent, inconsistent HTTP 206 / 416 behavior across Android OS versions | Explicit, deterministic HTTP header handling via Ktor |
| **Streaming Hashing (MD5 / SHA-256)** | **Zero-overhead**: Okio `HashingSink` piped during network stream | Requires secondary full-file read pass after download completes | **Zero-overhead**: Okio `HashingSink` piped during network stream |
| **Zip Extraction & Progress** | Shared safe zip unpacker with Zip-Slip protection and item progress | Custom platform decompression post-hooks | Shared safe zip unpacker with Zip-Slip protection and item progress |
| **Fallback URL Switching & Mirror Retry** | Built-in fallback routing (`UrlUtil.fallbackUrl()`) on HTTP/network errors | Difficult or impossible to dynamically switch URLs in Android `DownloadManager` | Built-in fallback routing (`UrlUtil.fallbackUrl()`) on HTTP/network errors |
| **Background Execution (App Suspended / Killed)** | Coroutine runs while process alive; killed if OS terminates process | Managed by OS daemon (`nsurlsessiond` on iOS, `DownloadManager` service on Android) | Android: `ForegroundService` / `WorkManager`. iOS: `beginBackgroundTask` / background `NSURLSession`. Desktop: Background Coroutine. |

### Evaluation & Decision

- **Android `DownloadManager` Rejection**: Android `DownloadManager` has severe limitations: cannot easily set dynamic retry fallback URLs, no streaming hash pipe, limited progress event granularity, cannot handle gapped multi-file batches efficiently, and frequently fails on private app storage paths.
- **Pure Ktor + Okio Shared Core**: Provides total control over HTTP headers, retry backoff, fallback mirrors, progress emission, file buffering, and on-the-fly checksum hashing.
- **Recommended Strategy: Option C (Hybrid Architecture)**:
  1. Build the unified **`DownloadEngine` in `commonMain`** using **Ktor Client** + **Okio**.
  2. Provide platform-specific lifecycle hosts:
     - **Android**: Host long downloads inside an Android Foreground Service with ongoing notification (`NotificationCompat.Builder`) to prevent OS process death. Use `WorkManager` for scheduled background audio update checks (`AudioUpdateWorker`).
     - **iOS**: Wrap active downloads with `UIApplication.shared.beginBackgroundTask` for background grace execution; optional background `NSURLSessionConfiguration` adapter for mega offline packs.
     - **Desktop (JVM)**: Background Kotlin Coroutine with desktop tray/progress indicator.

---

## 3. Core Architecture & Multiplatform Design

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          commonMain (Core Downloader)                       │
├─────────────────────────────────────────────────────────────────────────────┤
│  • DownloadManager / DownloadQueue (Concurrency limiter, FIFO / Priority)   │
│  • DownloadStrategy:                                                        │
│    - SingleFileDownloadStrategy (Database / Translation / Zip)              │
│    - GaplessAudioDownloadStrategy (Sura MP3s + Timing DB Zip)               │
│    - GappedAudioDownloadStrategy (Ayah MP3 batch)                           │
│  • KtorHttpDownloader (Byte-range resumption, 206/200/416, fallback mirror) │
│  • OkioSafeUnzipper (Zip-Slip check, compression bomb limit, item progress) │
│  • ChecksumVerifier (Okio HashingSink MD5 / SHA-256 verification)          │
│  • DownloadStateFlow (DownloadProgress: bytes, total, percent, speed, file)│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
│   androidMain   │           │     iosMain     │           │     jvmMain     │
├─────────────────┤           ├─────────────────┤           ├─────────────────┤
│ • Foreground    │           │ • Background    │           │ • Coroutine     │
│   Service       │           │   Task Wrapper  │           │   Worker Pool   │
│ • Notification  │           │   (beginBack-   │           │ • FileSystem    │
│   Manager       │           │   groundTask)   │           │   Paths         │
│ • WorkManager   │           │ • AppSupport    │           │                 │
│   Sync Worker   │           │   Directory     │           │                 │
└─────────────────┘           └─────────────────┘           └─────────────────┘
```

---

## 4. Key Mechanism Specifications

### 4.1. Resumable HTTP Downloads with Byte Ranges

1. **Partial File Convention**:
   - Write in-progress data to `<destination>/<filename>.part`.
   - Atomic rename to `<destination>/<filename>` only after complete download and successful hash/zip verification.
2. **HTTP Request Logic**:
   - If `<filename>.part` exists, read `downloadedBytes = partialFile.size`.
   - Add request header: `Range: bytes=${downloadedBytes}-`.
3. **HTTP Response Handling**:
   - **HTTP 206 (Partial Content)**: Server supports resume. Open Okio `Sink` in appending mode (`FileSystem.appendingSink(partPath)`). Total length is `downloadedBytes + response.contentLength()`.
   - **HTTP 200 (OK)**: Server ignored range header or file is new. Delete `.part`, start fresh from offset `0`.
   - **HTTP 416 (Range Not Satisfiable)**: Partial file size exceeds server resource or remote changed. Delete `.part` and restart from offset `0`.
   - **Network Disconnection / Timeout**: Flush and close sink. File remains intact for subsequent resume.

### 4.2. Zero-Overhead Streaming Hash Verification

Instead of reading multi-megabyte files from disk a second time to compute hashes:
- Chain Okio's `HashingSink` between the network source and the file sink:
  ```kotlin
  val fileSink = fileSystem.appendingSink(partPath).buffer()
  val hashingSink = HashingSink.md5(fileSink) // or sha256
  val bufferedHashingSink = hashingSink.buffer()
  
  // Stream data directly from Ktor ByteReadChannel to sink
  ktorChannel.readFully(bufferedHashingSink)
  bufferedHashingSink.flush()
  
  val calculatedMd5Hex = hashingSink.hash.hex()
  ```
- Compare `calculatedMd5Hex` against server-provided hash (e.g. from `AudioUpdateService`).

### 4.3. Safe Multiplatform Zip Decompression (`OkioSafeUnzipper`)

To prevent **Zip-Slip** (directory traversal vulnerability CERT IDS04-J) and **Zip-Bomb** (decompression denial-of-service):
1. **Canonical Path Guard**: Ensure target destination canonical path prefix matches:
   ```kotlin
   val destinationCanonical = fileSystem.canonicalize(destinationDir)
   val entryPath = destinationDir / entryName
   val entryCanonical = fileSystem.canonicalize(entryPath)
   require(entryCanonical.toString().startsWith(destinationCanonical.toString())) {
       "Zip slip security exception: $entryName"
   }
   ```
2. **Safety Ceilings**:
   - `MAX_FILES = 12,000` (reject zips exceeding entry limit).
   - `MAX_UNZIPPED_SIZE = 500 * 1024 * 1024` (500MB safety ceiling).
3. **Multiplatform Implementation**:
   - Okio 3.x+ provides multiplatform `FileSystem.openZip(zipPath)`. All platforms (Android, iOS, JVM) can read zip entries, stream contents, and emit progress callbacks via shared Kotlin code.

### 4.4. Fallback URL & Mirror Switching Strategy

Legacy `UrlUtil.kt` uses secondary mirrors when downloads fail. KMP downloader formalizes this:
```kotlin
class ResilientDownloader(
    private val httpClient: HttpClient,
    private val mirrorProvider: (String) -> List<String>
) {
    suspend fun downloadWithFallback(
        url: String,
        destination: Path,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        val mirrors = mirrorProvider(url) // [primaryUrl, fallbackUrl1, fallbackUrl2]
        var lastError: Throwable? = null
        
        for ((index, currentUrl) in mirrors.withIndex()) {
            try {
                if (index > 0) delay(1000L * index) // Backoff before fallback
                return downloadFile(currentUrl, destination, onProgress)
            } catch (e: Exception) {
                lastError = e
                if (e is CancellationException) throw e
            }
        }
        return DownloadResult.Failure(lastError ?: IOException("All mirrors failed"))
    }
}
```

---

## 5. Multiplatform Interface Design

### 5.1. Common Download Models and State

```kotlin
package io.github.parkiyong.quran.shared.download

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Queued(val downloadKey: String) : DownloadStatus
    data class Downloading(
        val downloadKey: String,
        val currentFileIndex: Int,
        val totalFiles: Int,
        val currentFileName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Float
    ) : DownloadStatus
    data class Unzipping(
        val downloadKey: String,
        val processedEntries: Int,
        val totalEntries: Int
    ) : DownloadStatus
    data class Success(val downloadKey: String) : DownloadStatus
    data class Failed(
        val downloadKey: String,
        val errorCode: DownloadErrorCode,
        val message: String
    ) : DownloadStatus
    data class Cancelled(val downloadKey: String) : DownloadStatus
}

enum class DownloadErrorCode {
    NETWORK_ERROR,
    DISK_FULL,
    PERMISSION_DENIED,
    CHECKSUM_MISMATCH,
    CORRUPTED_ZIP,
    CANCELLED
}
```

### 5.2. Downloader Service Interface

```kotlin
package io.github.parkiyong.quran.shared.download

import kotlinx.coroutines.flow.StateFlow

interface MultiplatformDownloader {
    val downloadStatus: StateFlow<DownloadStatus>

    suspend fun downloadMushafPack(widthParam: String): Result<Unit>
    suspend fun downloadAyahInfo(widthParam: String): Result<Unit>
    suspend fun downloadGaplessSuraBatch(qariId: Int, suras: List<Int>, downloadDatabase: Boolean): Result<Unit>
    suspend fun downloadGappedAyahBatch(qariId: Int, startSura: Int, endSura: Int): Result<Unit>
    suspend fun downloadTranslation(translationId: Int, fileUrl: String, filename: String): Result<Unit>
    
    fun cancelDownload(downloadKey: String)
    fun cancelAll()
}
```

---

## 6. Migration and Integration Plan for KMP

1. **Gradle Dependencies**:
   - `io.ktor:ktor-client-core:3.x`
   - Platform engines: `ktor-client-okhttp` (Android), `ktor-client-darwin` (iOS), `ktor-client-cio` or `java` (JVM/Desktop).
   - `com.squareup.okio:okio:3.x` (Multiplatform FileSystem, HashingSink, openZip).
2. **Phase 1: Shared Core Engine (`shared/commonMain`)**:
   - Port `ZipUtils.java` logic to multiplatform `OkioSafeUnzipper.kt`.
   - Implement `KtorHttpDownloader` with byte-range resume, HashingSink integration, and mirror fallback.
   - Implement `GaplessDownloadStrategy` and `SingleFileDownloadStrategy`.
3. **Phase 2: Platform Integration**:
   - **Android**: Adapt legacy `QuranDownloadService` to consume shared `MultiplatformDownloader` inside a foreground service. Keep `AudioUpdateWorker` with WorkManager.
   - **iOS**: Connect downloads to `UIApplication.shared.beginBackgroundTask` and iOS app lifecycle hooks.
   - **Desktop**: Integrate download progress into Compose Multiplatform status bar / UI dialogs.

---

## Primary Sources & References
- Legacy `quran_android` Download & Asset Engine:
  - `app/src/main/java/com/quran/labs/androidquran/service/QuranDownloadService.kt`
  - `app/src/main/java/com/quran/labs/androidquran/service/download/GaplessDownloadStrategy.kt`
  - `app/src/main/java/com/quran/labs/androidquran/service/download/GappedDownloadStrategy.kt`
  - `app/src/main/java/com/quran/labs/androidquran/service/download/SingleFileDownloadStrategy.kt`
  - `app/src/main/java/com/quran/labs/androidquran/util/ZipUtils.java`
  - `feature/audio/src/main/java/com/quran/labs/androidquran/feature/audio/util/MD5Calculator.kt`
  - `app/src/main/java/com/quran/labs/androidquran/worker/AudioUpdateWorker.kt`
  - `common/download/src/main/java/com/quran/mobile/common/download/Downloader.kt`
- Ktor HTTP Client Documentation: https://ktor.io/docs/client-engines.html
- Okio Multiplatform FileSystem & Hashing: https://square.github.io/okio/
- CERT Oracle Secure Coding Standard for Java (IDS04-J Zip Slip): https://wiki.sei.cmu.edu/confluence/display/java/IDS04-J.+Safely+extract+files+from+ZipInputStream
