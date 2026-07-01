# Quest OpenPDF Viewer

Small Android MVP for Meta Quest with package `com.orgista.openpdf`.

What is implemented:

- open a PDF through the Android document picker
- render pages with Android `PdfRenderer`
- previous/next page navigation
- a resizable floating reading panel
- a dedicated side-panel activity for thumbnails and document overview
- Quest-oriented shell metadata for default sizing and free resizing limits
- skeleton loading states instead of blank rendering gaps
- `android:resizeableActivity="true"` so Quest can treat it like a 2D resizable Android app

What is not implemented yet:

- native OpenXR session management
- stereo composition, controller input, or anchored 3D placement
- annotations, search, bookmarks, or the full upstream OpenPDF feature set

Build:

```bash
./gradlew test
./gradlew assembleDebug
```

Notes:

- This app is intentionally separate from the upstream `LibrePDF/OpenPDF` source tree because upstream is a Java PDF library, not an Android Quest application.
- The current MVP uses Android's platform PDF renderer because that is the most reliable path on Quest for a minimal reader. The code is structured so a later OpenXR layer can consume page bitmaps as textures.
- The side panel and window metadata are inspired by Quest shell patterns observed in a locally analyzed Meta-provided Instagram Quest APK, but no proprietary code or assets were copied.
