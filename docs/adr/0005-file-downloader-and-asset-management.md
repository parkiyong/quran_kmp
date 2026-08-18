# 5. Multiplatform File Downloader & Asset Management

Date: 2026-03-31

## Status

Accepted

## Context

The app must download large Mushaf page packs, SQLite databases, translation catalogs, and multi-file audio recitations with byte-range resume, streaming checksums, and background execution.

## Decision

1. **Shared Engine in `commonMain`**:
   - `Ktor HTTP Client` with byte-range resume headers (`Range: bytes=X-`, handling HTTP 206 / 200 / 416).
   - `Okio FileSystem` for file I/O, `.part` atomic staging, and inline `HashingSink` (MD5/SHA256) stream verification.
   - Built-in safe multiplatform zip extraction with Zip-Slip protection.

2. **Platform Lifecycle Integration**:
   - **Android**: Android Foreground Service (`QuranDownloadService`) with notifications + `WorkManager` for audio updates.
   - **iOS**: `UIApplication.shared.beginBackgroundTask` wrapper.
   - **Desktop (JVM)**: Background Coroutine worker pool.

## Consequences

- ~90% code sharing for download queuing, retry backoff, mirror switching, and file integrity verification.
- Replaces Android `DownloadManager` with deterministic cross-platform engine.
