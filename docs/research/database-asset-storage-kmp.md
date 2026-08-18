# Research: Legacy Database & Asset Storage Strategy in KMP

Investigates issue #2: How legacy SQLite database files (Ayah coordinates, translations, tafsir, bookmarks, timing) and image asset packs in `quran_android` are bundled, queried, and migrated across Android, Desktop (JVM), and iOS in Kotlin Multiplatform (KMP).

---

## 1. Inventory of Legacy Data & Asset Files

Analysis of legacy source repository `C:/Users/wraja/GitHub/quran_android`:

| Data Asset | Legacy Source Format | Legacy Access Pattern | Target Storage / Bundling in KMP |
| :--- | :--- | :--- | :--- |
| **Bookmarks & Tags** (`bookmarks.db`) | SQLite (3 schema versions: `bookmarks`, `tags`, `bookmark_tag`, `last_pages`) | `AndroidSqliteDriver` via SQLDelight (`com.quran.mobile.bookmark`) | Local App Database (Read/Write). Bundled empty / initial schema created on first run. |
| **Translations Catalog** (`translations.db`) | SQLite (5 schema versions: `translations`) | `AndroidSqliteDriver` via SQLDelight (`com.quran.mobile.translation`) | Local App Database (Read/Write). Bundled initial catalog or downloaded from API. |
| **Downloaded Translations & Tafsir** (`quran.<lang>.<id>.db`) | SQLite (`verses`, `properties` tables, schema v1-v2) | Custom `DatabaseHandler.kt` with raw Android `SQLiteDatabase` | Dynamic download per user request into app local storage. Read-only dynamic SQLite connections. |
| **Arabic Script DBs** (`quran.ar.uthmani.v2.db`, etc.) | SQLite (`arabic_text`, `share_text`, `properties`) | Dynamic SQLite file in storage, fallback copied from assets for some mushafs | Read-only SQLite db dynamically loaded or bundled as initial database. |
| **Ayah Coordinate DBs** (`ayahinfo_<width>.db`) | SQLite (`glyphs`, `ayah_markers`, `sura_headers`) | Custom `AyahInfoDatabaseHandler.java` / SQLDelight (`LineByLineAyahInfoDatabase`) | Downloaded per screen density / mushaf or bundled. Query-heavy spatial/rect lookups. |
| **Audio Timing DBs** (`<qari_db>.db`) | SQLite (`timings`, `properties`) | `SuraTimingDatabaseHandler.kt` | Downloaded on demand when audio recitation starts. Read-only SQLite query by `sura`. |
| **Word Alignment DB** (`word_alignment.db`) | SQLite asset (`word_alignment`) | `ImlaeiUthmaniDatabaseProvider.kt` via SQLDelight | Bundled in assets / Compose Resources, copied to app cache on first access. |
| **Mushaf Page Images** (`images_<width>.zip` / patches) | ZIP archive of `page<NNN>.png` (ALPHA_8 bitmaps) | Downloaded to file directory (`width<widthParam>/page<NNN>.png`), unzipped via `QuranFileUtils` | Downloaded on demand into application data directory or bundled fallback. |

---

## 2. KMP Database Engine Options: SQLDelight vs Room KMP vs Raw SQLite Driver

### Option Comparison

| Feature / Requirement | **SQLDelight** (Cash App) | **Room KMP** (AndroidX) | **Raw SQLite Driver** (e.g. `sqlite-driver` / cinterop) |
| :--- | :--- | :--- | :--- |
| **KMP Platform Support** | Android, JVM (Desktop), iOS (Native), macOS, Wasm/JS | Android, JVM (Desktop via SQLite/Xerial), iOS (Native via sqlite3) | Direct C-interop on iOS/Native, JDBC on JVM, `android.database.sqlite` on Android |
| **Schema & SQL Verification** | Compile-time validation of pure SQL (`.sq` files) | Annotation processing (KSP) over Kotlin entity/DAO classes | None (runtime SQL queries only) |
| **Static Predefined Schemas (Bookmarks, Translations Catalog)** | **Excellent**: Already used in `quran_android` (`Bookmark.sq`, `translations.sq`, `AyahGlyphs.sq`). | Good, but requires migrating `.sq` files to `@Entity` / `@Dao` classes and KSP setup. | High boilerplate for DAOs. |
| **Dynamic Multiple Databases (Downloaded Translations/Tafsir/Audio DBs)** | Supported via dynamic `SqlDriver` connection factory with same schema. | Unwieldy: Room expects a static `@Database` definition per distinct database instance. | Trivial opening of arbitrary DB paths, but no type safety. |
| **Legacy Code Reuse** | **Highest**: Direct reuse of `.sq` files and test infrastructure from `quran_android`. | Requires full rewrite of data layer into Room DAOs. | Requires writing custom cursor wrappers across platforms. |

### Decision & Recommendation
- **Standard Storage & Structured Schemas**: Use **SQLDelight**.
  - SQLDelight matches the existing migrations in `quran_android` (`Bookmark.sq`, `translations.sq`, `AyahGlyphs.sq`).
  - Drivers:
    - **Android**: `AndroidSqliteDriver`
    - **Desktop (JVM)**: `JdbcSqliteDriver` (`"jdbc:sqlite:" + path`)
    - **iOS**: `NativeSqliteDriver` (builds directly on iOS system `libsqlite3.dylib`)
- **Dynamic Read-Only DBs (Arbitrary Translations & Audio Timings)**:
  - Create reusable SQLDelight database definitions for generic translation files (`verses.sq`) and audio timing files (`timings.sq`).
  - Instantiate driver instances dynamically pointing to the downloaded database file path via a platform `SqlDriverFactory`.

---

## 3. Asset & File Storage Strategy Across Platforms

### Local Storage Directories Architecture

| Platform | Database / Cache Storage Directory | Bundled Read-Only Assets |
| :--- | :--- | :--- |
| **Android** | `context.noBackupFilesDir` or `context.filesDir` (Databases / Downloaded images) | `assets/` or Compose Resources (`compose.components.resources`) |
| **Desktop (JVM)** | `~/.local/share/QuranApp` (Linux), `~/Library/Application Support/QuranApp` (macOS), `%APPDATA%/QuranApp` (Windows) | Embedded JAR resources / Compose Multiplatform resources |
| **iOS** | `FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)` / `.cachesDirectory` | App Bundle Resources / Compose Resources |

### File Storage Abstraction: `QuranFileManager`
Define a common Multiplatform interface:
```kotlin
interface QuranFileManager {
    fun getDatabaseDirectory(): String
    fun getImagesDirectory(widthParam: String): String
    fun getAudioDirectory(): String
    fun getAyahInfoDirectory(): String
    fun copyFromResources(resourcePath: String, destinationPath: String)
}
```

---

## 4. Migration & Ingestion Strategy

1. **Pre-populated & Packaged Databases**:
   - Pack static mapping assets (`word_alignment.db`, default translations metadata) into Compose Resources (`composeApp/src/commonMain/composeResources/`).
   - On first launch, `QuranFileManager.copyFromResources()` writes them to the platform application support directory so SQLDelight drivers can access them by file path.

2. **Legacy User Data Migration (Bookmarks & Recents)**:
   - On Android: Existing SQLite / SQLDelight files in internal storage retain their paths; SQLDelight schema migrations (`1.sqm`, `2.sqm`, `3.sqm`) handle incremental version upgrades.
   - Cross-Platform Sync: For desktop and iOS, import bookmarks via the standard JSON backup/export format already used in `BookmarkBackupImportNormalizer.kt`.

3. **Page Images & Coordinate Data Download**:
   - Keep the existing HTTP zip download + stream unzip pipeline.
   - Decompress downloaded zip packs into `getImagesDirectory(widthParam)` and `getAyahInfoDirectory()`.

---

## Primary Sources & References
- Legacy `quran_android` SQLDelight definitions:
  - `common/bookmark/src/main/sqldelight/com/quran/mobile/bookmark/Bookmark.sq`
  - `common/translation/src/main/sqldelight/com/quran/mobile/translation/translations.sq`
  - `common/linebyline/data/src/main/sqldelight/com/quran/mobile/linebyline/data/AyahGlyphs.sq`
- Legacy Database Handlers:
  - `app/src/main/java/com/quran/labs/androidquran/database/DatabaseHandler.kt`
  - `app/src/main/java/com/quran/labs/androidquran/data/AyahInfoDatabaseHandler.java`
  - `app/src/main/java/com/quran/labs/androidquran/database/SuraTimingDatabaseHandler.kt`
- Legacy File & Asset Management:
  - `app/src/main/java/com/quran/labs/androidquran/util/QuranFileUtils.kt`
  - `common/data/src/main/java/com/quran/data/core/QuranFileManager.kt`
