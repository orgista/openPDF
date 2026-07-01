# Forking and OpenXR Notes

This workspace includes the upstream `LibrePDF/OpenPDF` archive, but upstream is not a Quest/OpenXR app.

Practical conclusions for this fork:

- You can fork OpenPDF. Its README and `LICENSE.md` state the project is dual-licensed as `MPL-2.0 OR LGPL-2.1+`.
- You do not have to ship every upstream feature in your fork. You can build a narrower product that only keeps PDF reading and a resizable panel.
- You should preserve copyright and license notices from any upstream files you actually copy or modify.
- If you choose the MPL path, file-level copyleft applies to modified MPL-covered files. New files in your app can stay under your own license.
- If you choose the LGPL path, keep the library boundary clear and preserve the user's ability to replace the LGPL-covered library.

OpenXR status:

- I found no OpenXR code in the upstream archive.
- The upstream README mentions Android support, creation, editing, and rendering, but it does not expose a Meta Quest OpenXR viewer.
- This MVP therefore targets Quest as a resizable Android app first and leaves OpenXR as a follow-on native integration step.

Quest shell patterns now applied in this app:

- separate main reader and side-panel activities
- default window sizes and free-resize ranges declared in the manifest
- translucent side-panel shell theme
- loading skeletons for async content instead of empty flashes
