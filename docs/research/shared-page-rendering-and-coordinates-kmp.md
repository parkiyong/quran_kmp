# Research: Shared Page Rendering & Coordinate Highlighting in Compose Multiplatform

Investigates issue #4: How Compose Multiplatform (Android, Desktop JVM, iOS) renders Mushaf page images, executes custom Ayah highlight coordinate polygons / glyph clipping, and coordinates single-page mobile vs dual-page tablet/desktop layouts.

---

## 1. Legacy Architecture Analysis (`quran_android`)

In `quran_android`, page presentation and coordinate interaction are implemented via custom Views and canvas manipulation:

1. **Image Layout & Sizing**:
   - `QuranImagePageLayout` (`QuranPageLayout.java`, `QuranImagePageLayout.kt`, `QuranTabletImagePageLayout.kt`): Coordinates image loading and overlays.
   - `HighlightingImageView` extends `AppCompatImageView`: Handles coordinate mapping (`Matrix.mapRect`), night mode color matrix filters, and draws custom overlays on Android `Canvas`.
2. **Ayah Highlight Rendering**:
   - `HighlightsDrawer.kt` executes two passes around image drawing:
     - **Pre-draw Clipping / Color Filter Pass**: Highlights with mode `COLOR` or `HIDE` clip the canvas or re-filter the image canvas to mask/re-color glyph regions before the main bitmap draws.
     - **Post-draw Overlay Pass**: Highlights with mode `HIGHLIGHT` (selection), `BACKGROUND` (audio recitation tracking), or `UNDERLINE` draw rectangles (`canvas.drawRect(scaledRect, paint)`) directly over the bitmap.
   - Highlighting coordinates originate from SQLite (`AyahInfoDatabaseHandler.java` querying `glyphs` table: `min_x`, `min_y`, `max_x`, `max_y`, `line_number`, `sura_number`, `ayah_number`, `position`).
   - `PageGlyphsCoords.kt` aggregates raw bounding boxes, expands line bounds horizontally to the edge of the Mushaf page, and expands vertical spacing between lines for seamless selection highlight rectangles.
3. **Dual Page & ViewPager Mechanics**:
   - `ViewPager` with `QuranPageAdapter` / `TabletFragment`: Computes position offsets using `QuranInfo.getPageFromPosition(pos, isDualPagesVisible)` and `QuranInfo.getPositionFromPage(page, isDualPagesVisible)`.
   - Right-to-Left (RTL) reading order: Page `N` appears on the right and `N+1` on the left in dual-page mode.

---

## 2. Compose Multiplatform Rendering & Coordinate Architecture

### 2.1 Page Image Loading & Drawing

Across Android, JVM Desktop, and iOS, page images (`page<NNN>.png` or custom bitmap formats) can be rendered efficiently using native Compose Multiplatform graphics primitives:

- **Async Image Loading**:
  - Use `AsyncImage` / `rememberAsyncImagePainter` from **Coil 3** (`io.coil-kt.coil3:coil-compose`) or standard Compose image decoders backed by Skiko / Skia on Desktop/iOS and Android graphics on Android.
  - Page images reside on disk in the application storage directory (managed by `QuranFileManager`).
- **Coordinate Space Scaling & Mapping**:
  - Mushaf page coordinate DBs use fixed source pixel coordinates (e.g. baseline width `1024` or `1260`).
  - In Compose, the image is rendered inside a `Box` with `ContentScale.Fit` (or `ContentScale.FillBounds` when aspect ratio is pre-locked).
  - Compute a standard `Matrix` / `ScaleFactor` transforming source coordinates $(x_{src}, y_{src})$ to composable canvas coordinates $(x_{canvas}, y_{canvas})$:
    $$\text{scale} = \min\left(\frac{W_{canvas}}{W_{source}}, \frac{H_{canvas}}{H_{source}}\right)$$
    $$\text{offsetX} = \frac{W_{canvas} - (W_{source} \times \text{scale})}{2}, \quad \text{offsetY} = \frac{H_{canvas} - (H_{source} \times \text{scale})}{2}$$

### 2.2 Custom Ayah Highlight Coordinate Drawing

Compose Multiplatform provides platform-agnostic graphics via `androidx.compose.ui.graphics`:
- `Canvas` composable / `Modifier.drawWithContent` / `Modifier.drawWithCache`.
- `drawRect`, `drawPath`, `clipPath`, `clipRect`, `BlendMode`, and `ColorFilter`.

#### Highlight Drawing Mechanics in Compose:

```kotlin
@Composable
fun QuranPageCanvas(
    pageImage: ImageBitmap,
    pageCoordinates: PageCoordinates?,
    activeHighlights: List<HighlightModel>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val srcWidth = pageImage.width.toFloat()
        val srcHeight = pageImage.height.toFloat()
        val scale = minOf(size.width / srcWidth, size.height / srcHeight)
        val dstWidth = srcWidth * scale
        val dstHeight = srcHeight * scale
        val offsetX = (size.width - dstWidth) / 2f
        val offsetY = (size.height - dstHeight) / 2f

        fun Rect.toCanvasRect(): Rect = Rect(
            left = offsetX + (left * scale),
            top = offsetY + (top * scale),
            right = offsetX + (right * scale),
            bottom = offsetY + (bottom * scale)
        )

        // 1. Draw Under-Page / Background Highlights (if clipping or tinting)
        for (highlight in activeHighlights.filter { it.type.mode == HighlightMode.BACKGROUND }) {
            for (rect in highlight.bounds) {
                drawRect(
                    color = highlight.color,
                    topLeft = Offset(offsetX + rect.left * scale, offsetY + rect.top * scale),
                    size = Size(rect.width * scale, rect.height * scale),
                    blendMode = BlendMode.SrcOver
                )
            }
        }

        // 2. Draw Main Page Image
        drawImage(
            image = pageImage,
            dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
            dstSize = IntSize(dstWidth.roundToInt(), dstHeight.roundToInt())
        )

        // 3. Draw Over-Page Highlights (Selection, Underline, Overlay)
        for (highlight in activeHighlights.filter { it.type.mode == HighlightMode.HIGHLIGHT }) {
            for (rect in highlight.bounds) {
                drawRect(
                    color = highlight.color,
                    topLeft = Offset(offsetX + rect.left * scale, offsetY + rect.top * scale),
                    size = Size(rect.width * scale, rect.height * scale),
                    blendMode = BlendMode.Multiply // Ensures text beneath remains legible
                )
            }
        }
    }
}
```

- **Touch & Click Detection**:
  - `Modifier.pointerInput(Unit)` with `detectTapGestures` intercepts tap offsets $(x, y)$.
  - Invert the canvas scale matrix:
    $$x_{src} = \frac{x - \text{offsetX}}{\text{scale}}, \quad y_{src} = \frac{y - \text{offsetY}}{\text{scale}}$$
  - Spatial lookup in `PageGlyphsCoords` / `AyahCoordinates` determines which Ayah or word was tapped.

---

## 3. Responsive Page Layout: Single vs Dual-Page Mode

### 3.1 Adaptive Screen Detection

In Compose Multiplatform:
- Use `BoxWithConstraints` or Material 3 `WindowWidthSizeClass` (Compact, Medium, Expanded).
- **Single-Page Mode**: Triggered when `windowWidthSizeClass == WindowWidthSizeClass.Compact` (e.g. mobile portrait, narrow split-screen).
- **Dual-Page Mode**: Triggered when `windowWidthSizeClass == WindowWidthSizeClass.Expanded` (Tablets landscape, Desktop, foldable unfolded).

### 3.2 Dual-Page & Single-Page Paging with `HorizontalPager`

Compose Multiplatform provides `androidx.compose.foundation.pager.HorizontalPager`:
- Support RTL page progression naturally via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.

```kotlin
@Composable
fun QuranReaderPager(
    quranInfo: QuranInfo,
    isDualPage: Boolean,
    pagerState: PagerState,
    onAyahSelected: (SuraAyah) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            if (isDualPage) {
                // Dual page: calculate right (first) and left (second) pages
                val rightPage = quranInfo.getPageFromPosition(pageIndex, isDualPagesVisible = true)
                val leftPage = rightPage + 1

                Row(modifier = Modifier.fillMaxSize()) {
                    // In RTL: first element is on the right, second element is on the left
                    QuranPageView(
                        pageNumber = rightPage,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onAyahSelected = onAyahSelected
                    )
                    if (leftPage <= quranInfo.numberOfPages) {
                        QuranPageView(
                            pageNumber = leftPage,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onAyahSelected = onAyahSelected
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                val singlePage = quranInfo.getPageFromPosition(pageIndex, isDualPagesVisible = false)
                QuranPageView(
                    pageNumber = singlePage,
                    modifier = Modifier.fillMaxSize(),
                    onAyahSelected = onAyahSelected
                )
            }
        }
    }
}
```

### 3.3 Page Index Mapping Logic (`QuranInfo`)

Porting `QuranInfo` logic verbatim to `commonMain`:
- Total pages in dual mode: `ceil(numberOfPages / 2)`.
- Seamless switching: When resizing window from Single $\leftrightarrow$ Dual, convert `currentPage` via `getPositionFromPage(currentPage, isDualPagesVisible)`.

---

## 4. Implementation Recommendations & Tradeoffs

1. **Graphics Pipeline**:
   - `Modifier.drawWithContent` / `Canvas` in Compose UI replaces legacy Android `ImageView` subclassing and custom `onDraw(Canvas)` hooks cleanly without needing Skiko-specific native calls.
   - Use `BlendMode.Multiply` for selection highlight overlay to avoid obscuring dark Quranic glyphs without requiring complex multi-pass clip operations.
2. **Page Caching**:
   - Implement an in-memory LRU cache of decoded `ImageBitmap` instances (retaining 4-6 pages in RAM) to keep `HorizontalPager` scrolling at 60/120 FPS across JVM, Android, and iOS.
3. **Multiplatform Geometry Models**:
   - Replace `android.graphics.RectF` with `androidx.compose.ui.geometry.Rect` in `PageCoordinates`, `AyahBounds`, and `PageGlyphsCoords` in `commonMain`.

---

## Primary Sources & References
- Legacy Drawing & Coordinate System:
  - `quran_android`: `app/src/main/java/com/quran/labs/androidquran/view/HighlightingImageView.java`
  - `quran_android`: `app/src/main/java/com/quran/labs/androidquran/view/HighlightsDrawer.kt`
  - `quran_android`: `common/pages/src/main/java/com/quran/page/common/data/coordinates/PageGlyphsCoords.kt`
  - `quran_android`: `common/pages/src/main/java/com/quran/page/common/data/AyahCoordinates.kt`
- Legacy Paging & Sizing:
  - `quran_android`: `common/data/src/main/java/com/quran/data/core/QuranInfo.kt`
  - `quran_android`: `app/src/main/java/com/quran/labs/androidquran/ui/PagerActivity.kt`
  - `quran_android`: `app/src/main/java/com/quran/labs/androidquran/util/QuranUtils.java`
- Compose Multiplatform UI Docs:
  - `androidx.compose.foundation.pager.HorizontalPager`
  - `androidx.compose.ui.graphics.Canvas` and `androidx.compose.ui.geometry.Rect`
