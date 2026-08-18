# Quran KMP Context

## Domain Glossary

- **Mushaf**: Physical or digital rendition of Quranic pages (Madani, IndoPak, Tajweed, etc.) with fixed page coordinates/layouts.
- **Ayah**: A verse in the Quran, uniquely identified by Surah number and Ayah number (e.g. 1:1).
- **Surah**: A chapter in the Quran (1 to 114).
- **Juz'**: One of 30 parts of the Quran.
- **Page Marker / Coordinate Map**: Mapping bounding boxes or point coordinates on a page image/canvas to specific Ayahs for selection and highlighting.
- **Audio Reciter / Recitation**: Audio streams or downloaded files of verse-by-verse recitation with timing data for playback synchronization.
- **Tafsir**: Exegesis/commentary associated with specific Ayahs.
- **Translation**: Textual rendering of Ayah meaning in different languages.
- **Reader Layout Mode**: Dynamic page display mode driven by window width breakpoints (Single Page for Compact/Medium; Dual Page for Expanded Desktop/Tablet).
- **Navigation Route**: Type-safe destination declaration handled uniformly by AndroidX Navigation Compose across Android, iOS, and Desktop.
- **Ayah Info Database (`ayahinfo_*.db`)**: Read-only SQLite file containing page Ayah coordinates, line markers, and bounding boxes for visual overlay.
- **Translation Database (`quran.<lang>.db`)**: Downloaded SQLite file with translated verse text indexed by surah and ayah.
- **Timing Database**: SQLite database containing millisecond timing intervals for Ayah synchronization during recitation playback.


