<p align="center">
  <img alt="openPDF icon" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96">
</p>

<h1 align="center">openPDF</h1>

<h3 align="center">A PDF reader for Meta Quest</h3>

<p align="center">
  <a href="#install">Install</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="https://orgista.com">orgista.com</a>
</p>

<p align="center">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-2ea44f?style=flat-square"></a>
  <a href="#compatibility"><img alt="Platform: Meta Quest" src="https://img.shields.io/badge/platform-Meta%20Quest-1c1e20?style=flat-square"></a>
  <a href="#compatibility"><img alt="Min API 29" src="https://img.shields.io/badge/min%20API-29-3ddc84?style=flat-square"></a>
</p>

openPDF opens PDF files in a resizable floating panel on Meta Quest, with a companion side panel for page thumbnails. This is an early release (version 0.1.0): it covers reading, and leaves annotation and search for later.

## Features

- Open any PDF through the Android document picker
- Render pages with the platform `PdfRenderer`
- Previous and next page navigation
- Six zoom steps from 85% to 175%
- Dark reading mode, remembered between launches
- Bookmark the current document, page, zoom, and panel size, and resume from it on the next launch
- Width and height sliders for the reading panel, plus free resizing in the Quest shell
- Side panel that shows thumbnails of up to six pages around the current one
- Skeleton placeholders while pages load

## Install

There are no published releases yet. Build the APK (see below), then install it over ADB with your headset in developer mode:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app installs as `com.orgista.openpdf`.

## Compatibility

| Device | Minimum | Notes |
| --- | --- | --- |
| Meta Quest | Android API 29 | Runs as a resizable 2D window; landscape only |

The manifest does not restrict the app to specific Quest models. It declares window sizing for the Quest shell: the reader opens at 1024 x 768 dp, the side panel at 420 x 768 dp.

## Not yet supported

- Native OpenXR sessions, stereo rendering, and controller-anchored placement
- Annotations and text search
- Multiple bookmarks (one saved position at a time)

## Build from source

Prerequisites: Java 17, Android SDK 34, and the Gradle wrapper included in this repo.

```bash
git clone https://github.com/orgista/openPDF.git
cd openPDF
./gradlew test
./gradlew assembleDebug
```

| Command | Description |
| --- | --- |
| `./gradlew assembleDebug` | Debug APK at `app/build/outputs/apk/debug/` |
| `./gradlew test` | Unit tests |

## Privacy

Documents are opened and rendered on the device. openPDF declares no network permission and has no account, backend, or analytics.

## More from orgista

- [OpenQR](https://github.com/orgista/openQR): a QR code reader for Meta Quest 3 and 3S

## Contributing

Issues and pull requests are welcome. Keep changes small and describe how you tested them on a headset.

## License

[MIT](LICENSE)
