# 2. Multiplatform SQLite Database & Asset Storage Strategy

Date: 2026-03-31

## Status

Accepted

## Context

`quran_android` relies on two distinct database categories:
1. **Dynamic / Schema-managed DBs**: Bookmarks, tags, download status, audio timing markers, line-by-line metadata, and translation indexing.
2. **Read-only / Pre-packaged / Downloaded SQLite DBs**: `ayahinfo_*.db` (page ayah coordinates and bounding boxes), `quran.ar.db` (Arabic text), translation/tafsir databases (`quran.<lang>.db`), and sura timing databases (`<sura>_<qari>.db`).

In KMP across Android, iOS, and Desktop:
- Android uses `AndroidSqliteDriver`.
- iOS / Native uses `NativeSqliteDriver`.
- Desktop (JVM) uses `JdbcSqliteDriver` (`jdbc:sqlite:<path>`).

## Decision

1. **SQLDelight 2.x as Unified Multiplatform ORM**:
   - Port all existing `.sq` schemas from `quran_android` directly (`Bookmark.sq`, `BookmarkTag.sq`, `Tag.sq`, `LastPage.sq`, `AyahGlyphs.sq`, `AyahHighlight.sq`, `AyahMarker.sq`, `SuraHeader.sq`, `translations.sq`, `WordAlignment.sq`).
   - Use `app.cash.sqldelight:runtime` with multiplatform platform drivers (`AndroidSqliteDriver`, `NativeSqliteDriver`, `JdbcSqliteDriver`).

2. **Unified Raw SQLite Driver for Read-Only / Dynamic Downloaded DBs**:
   - Provide a common multiplatform wrapper `QuranDatabaseDriverFactory` / `SqliteDatabaseConnection` using SQLDelight driver primitives to open downloaded read-only databases at arbitrary runtime filesystem paths (`ayahinfo_*.db`, translation `.db`, sura audio timing `.db`).
   - Eliminates direct dependencies on `android.database.sqlite.SQLiteDatabase`.

3. **Multiplatform Filesystem & Asset Storage Abstraction**:
   - Use `okio.Path` and `okio.FileSystem` (`FileSystem.SYSTEM`) for all cross-platform file paths and directory management:
     - Android: `context.filesDir` / `context.getExternalFilesDir()`
     - iOS: `NSDocumentDirectory` / `NSApplicationSupportDirectory`
     - Desktop: `~/.quran_kmp/` or standard OS application data directory (`AppData` on Windows, `Application Support` on macOS, `~/.local/share` on Linux).
   - Bundled assets (`word_alignment.db`, default metadata) packaged via Compose Multiplatform Resource library (`org.jetbrains.compose.resources`).

## Consequences

- 100% schema parity and reuse of existing SQL statements from `quran_android`.
- Clean separation from Android framework classes.
- Zero database format conversion required for downloaded translation and coordinate files.
