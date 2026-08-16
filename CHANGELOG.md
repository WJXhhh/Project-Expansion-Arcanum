# Changelog

## 1.1.4-arcanum.1 — Unreleased

### Maintenance

- Established Project Expansion: Arcanum as an independently maintained Minecraft 1.20.1 fork.
- Added the Gradle Wrapper and a minimal GitHub Actions build workflow.
- Made Java source encoding explicit for reproducible builds.
- Added baseline, attribution, migration, and issue-reporting documentation.

### Compatibility

- Preserved the `projectexpansion` runtime mod ID and existing resource namespace.
- Kept the upstream Forge `47.2.0` and ProjectE `PE1.0.1` baseline while maintenance verification is in progress.

### Branding

- Updated project metadata and documentation to Project Expansion: Arcanum.
- Redirected update checking to the Arcanum repository's update JSON.

### Fixed

- Preserved fractional Collector output so low-rate generation can accumulate instead of being truncated to zero.
- Allowed positive sub-1 EMC/t Collector output to transfer once a whole EMC has accumulated, including to Klein Stars.
