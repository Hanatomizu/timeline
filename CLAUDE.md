# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew :app:compileDebugKotlin # Just compile Kotlin
```

Clean all build artifacts when changing Room entities or migrations:

```bash
rm -rf app/build
```

## Architecture (MVVM)

```
Screen (Composable) → ViewModel (StateFlow) → Repository → Room DAO → SQLite
```

- **ViewModels** extend `AndroidViewModel` — use `viewModelScope.launch` for async, `StateFlow` + `collectAsState()` for reactive UI
- **Repository** wraps all DAOs — the single point for data operations
- **Room** with KSP — entities in `data/entity/`, DAOs in `data/dao/`, migration in `AppDatabase`
- **Navigation** via Compose Navigation in `MainActivity.kt`

## Key Patterns

### Image Handling
- All user images are copied to `context.filesDir/images/` via `ImageFileHelper.copyImageToInternal()`
- Display with `AsyncImage` (Coil) from the absolute file path
- Multi-image support: `EventImageEntity` sub-table, paths managed as `List<String>` in ViewModel
- Save to gallery: `ImageSaveHelper.saveImageToGallery()` via `MediaStore` (no permissions needed)
- Preview: `FullScreenImageGallery` with `HorizontalPager` + zoom/long-press-save

### Export as Image
- `ExportHelper.renderToBitmap()` attaches a `ComposeView` to the Activity decor view, measures the full content, and draws to `Bitmap`
- Images are pre-loaded synchronously via `Coil.imageLoader().execute()` before rendering, stored as `Map<String, Bitmap?>`
- Hardware bitmaps from Coil must be converted with `.copy(Bitmap.Config.ARGB_8888, false)` before drawing to a software `Canvas`

### Database Migrations
- `AppDatabase.MIGRATION_1_2` shows the pattern: create new table → copy data → drop old → rename → create indices
- When dropping columns, the `CREATE TABLE` replacement must include the same foreign keys and indices as the original entity definition

### Form State Pattern
- Each form field is a separate `MutableStateFlow` in the ViewModel (e.g., `_formContent`, `_formImagePaths`, `_formLabelColor`)
- The dialog reads these with `collectAsState()` and writes through ViewModel methods
- `saveEvent()` validates content synchronously, then launches a coroutine for DB writes

### Image File Cleanup
- When removing an image from the form: `ImageFileHelper.deleteImage()` immediately deletes the file
- When saving an edited event: old image files not in the new list are deleted
- When deleting an event: all associated image files are deleted before the DB record

## Dependencies (key versions)

| Library | Version |
|---------|---------|
| AGP | 9.1.0 |
| Kotlin | 2.1.20 |
| Gradle | 9.3.1 |
| Room | 2.7.2 (KSP) |
| Compose BOM | 2024.01.00 |
| Coil | 2.6.0 |
| KSP | 2.1.20-2.0.0 |

## Package Map

```
moe.hanatomizu.timeline/
├── MainActivity.kt            — Entry, navigation graph
├── data/
│   ├── entity/                — Room entities + EventWithImages relation
│   ├── dao/                   — DAO interfaces
│   ├── AppDatabase.kt         — Room DB + migrations
│   └── TimelineRepository.kt  — Unified data access
├── viewmodel/                 — Two ViewModels (list + detail)
├── ui/
│   ├── theme/Theme.kt         — Material3 dynamic color + dark/light
│   ├── screens/               — Two screens (list + detail)
│   └── components/            — FullScreenImageGallery, FullScreenImageView
└── util/
    ├── ColorPickerDialog.kt   — Preset color grid dialog
    ├── ExportHelper.kt        — Timeline → Bitmap export
    ├── ImageFileHelper.kt     — Copy to internal storage
    └── ImageSaveHelper.kt     — MediaStore save to gallery
```
