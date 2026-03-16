# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses tags like `v0.1.0` while changelog versions omit the
leading `v`.

## [Unreleased]

### Added

### Changed

### Fixed

### Documentation

### CI

- Aligned Maven Central publishing with Eleven19 org secrets by using the `ELEVEN19_*` Sonatype and base64 PGP credentials path.
- Fixed release extras packaging so the Unix `milli` launcher is retained in GitHub release assets alongside `milli.bat`.

## [0.2.0] - 2026-03-16

### Added

- Added a prerelease recommendation helper for maintainers to derive the next semantic prerelease version and channel.

### Changed

- Renamed the runtime package namespace and published Maven coordinates to `io.eleven19.mill-interceptor`.

### Fixed

### Documentation

- Documented the prerelease recommendation and dispatch flow for maintainers cutting releases.

### CI

- Added release workflow coverage for namespace publishing metadata and prerelease recommendation helpers.

## [0.1.0] - 2026-03-14

### Added

- Added Maven, Gradle, and sbt interceptor support with command parsing and task mapping.
- Added Scribe-backed logging and command-line entrypoint wiring for the interceptor app.

### Changed

- Added native release packaging support through Mill so platform archives can be published to GitHub Releases.

### Fixed

- Improved command translation coverage with focused unit tests across the Maven, Gradle, and sbt shims.

### Documentation

- Added contributor docs for native release packaging and release workflow behavior.

### CI

- Added GitHub CI for build and test validation.
