<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="OpenPDF icon" width="96">

# OpenPDF

**A PDF reader for Meta Quest, in a resizable panel with page thumbnails beside it.**

[![CI](https://img.shields.io/github/actions/workflow/status/orgista/openPDF/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/orgista/openPDF/actions/workflows/ci.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/orgista/openPDF/badge)](https://scorecard.dev/viewer/?uri=github.com/orgista/openPDF)
[![License: MIT](https://img.shields.io/badge/license-MIT-2ea44f?style=flat-square)](LICENSE)
[![Platform: Meta Quest](https://img.shields.io/badge/platform-Meta%20Quest-1c1e20?style=flat-square)](#compatibility)

[Install](#install) · [Build from source](#build-from-source) · [orgista.com](https://orgista.com)

</div>

---

OpenPDF opens PDF files in a resizable floating panel on Meta Quest, with a companion side panel for page thumbnails. This is an early release (version 0.1.0): it covers reading, and leaves annotation and search for later.

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

Documents are opened and rendered on the device. OpenPDF declares no network permission and has no account, backend, or analytics.

## More from orgista

- [OpenQR](https://github.com/orgista/openQR): a QR code reader for Meta Quest 3 and 3S

## Releases

Downloads live on the [Releases page](https://github.com/orgista/openPDF/releases): the signed APK.
The release APK is built and signed on our build Mac and published there.

- **Versions** follow [Semantic Versioning](https://semver.org/): `vMAJOR.MINOR.PATCH`. A version with a
  suffix (`v1.4.0-beta.1`) is a pre-release and is marked as one.
- **Cadence:** we ship when a change is ready, and at least one maintenance release a month while the project
  is in active development. Security fixes ship within 7 days of a confirmed report.
- **Every release** has a [CHANGELOG](CHANGELOG.md) entry (the release notes are taken from it), a `SHA256SUMS` checksum file and a GitHub attestation for every file.
- **Support:** only the latest release gets fixes. Please update before reporting a bug.

Check a download before installing it:

```bash
shasum -a 256 -c SHA256SUMS --ignore-missing
gh attestation verify openpdf-v0.1.0.apk --repo orgista/openPDF
```

**Maintainers:** add the version's section to `CHANGELOG.md`, build and sign the release, then publish with

```bash
scripts/release-github X.Y.Z path/to/build-artifact
```

The script tags `vX.Y.Z`, uses the CHANGELOG section as the notes, uploads the files with `SHA256SUMS`, and can be
re-run safely. Full policy: [orgista release process](https://github.com/orgista/.github/blob/main/RELEASING.md).

## Contributing

Issues and pull requests are welcome. Keep changes small and describe how you tested them on a headset.

## License

[MIT](LICENSE)
