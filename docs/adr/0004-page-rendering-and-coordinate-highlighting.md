# 4. Shared Page Rendering & Coordinate Highlighting

Date: 2026-03-31

## Status

Accepted

## Context

Mushaf display requires pixel-accurate rendering of Quran pages, dynamic night-mode color filters, drawing active Ayah highlight polygons, and dual-page presentation on desktop/tablets.

## Decision

1. **Rendering Canvas**:
   - Compose Multiplatform `Canvas` / `Modifier.drawWithContent` with matrix coordinate scaling from fixed image coordinates (1024/1260px) to screen canvas dimensions.
   - Blend modes: `BlendMode.Multiply` for selection/recitation overlays over page text; `ColorFilter` for night-mode inversion.

2. **Adaptive Layouts**:
   - `WindowWidthSizeClass.Expanded` activates Dual Page RTL mode (Page $N$ on right, $N+1$ on left).
   - `Compact` / `Medium` renders Single Page RTL mode with horizontal pager.

3. **Touch & Selection**:
   - `Modifier.pointerInput` with coordinate inverse transformation against `PageCoordinates` to resolve tapped Ayah/word.

## Consequences

- Completely shared reader engine across Android, iOS, and Desktop.
- Zero platform-specific Canvas dependencies (replacing `android.graphics.Canvas` / `android.graphics.Matrix`).
