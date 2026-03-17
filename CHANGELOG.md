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

## [0.3.0] - 2026-03-17

### Added

- Added a real multi-module Mill layout with `modules/mill-interceptor` and a published `mill-interceptor-maven-plugin` artifact.
- Added Maven plugin packaging and end-to-end integration coverage so the published plugin can be installed and invoked from Maven.

### Changed

- Reorganized the repository so the aggregate root coordinates module wiring while the `milli` product and Maven plugin publish from dedicated module paths.
- Switched module application and test code to Kyo-first filesystem and process boundaries while keeping `os-lib` as the preferred build-runtime abstraction in `mill-build/`.

### Fixed

- Removed deprecated Mill publish API usage from the custom publish modules and Maven plugin publish helpers.
- Suppressed the JDK 25 `sun.misc.Unsafe` warning for Mill and forked test workers without leaking unsupported JVM flags into nested Maven invocations.

### Documentation

- Added maintainer guidance for beads-aware worktrees and recovery steps for misconfigured or tracker-broken worktrees.
- Added contributor guidance on filesystem library preferences for Kyo-based module code versus `os-lib` in Mill build code.

### CI

- Aligned Maven Central publishing with Eleven19 org secrets by using the `ELEVEN19_*` Sonatype and base64 PGP credentials path.
- Switched Maven Central publishing tasks to Mill's `publishSonatypeCentral` surface so release jobs use the same Central publishing path as `ascribe`.
- Fixed release extras packaging so the Unix `milli` launcher is retained in GitHub release assets alongside `milli.bat`.
- Added manual `workflow_dispatch` support for the Maven Central publish workflow so release publishing can be retried without a new tag.

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
