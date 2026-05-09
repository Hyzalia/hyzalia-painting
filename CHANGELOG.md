# Changelog

## 1.1.0

### Added

- **`/prefabsavefluid`** — Opens the same prefab save UI as the vanilla Builder Tools flow, then rewrites the saved `.prefab.json` with **fluids** sampled from the world (water, etc.) for the current selection.
- Enrichment runs after the normal `saveFromSelection` pass, so behavior matches the stock save except that fluid data is always filled in, including when using an **Editor_Anchor** block or **player anchor** (paths that otherwise skip fluids).

### Fixed

- **Clone / preview** — Fluids in the saved file are stored in **local** coordinates aligned with the **block grid** (minimum local block position + offset within the world selection). Fixes previews that showed no water when bounds in the file were world-space or misleading.
- **Empty fluids section** — Removed an early exit that skipped enrichment when deserialized selection bounds were `(0,0,0)` for some anchor saves.

### Changed

- Bump release version to **1.1.0**.
