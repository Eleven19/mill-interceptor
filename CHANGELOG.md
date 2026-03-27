# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses tags like `v0.1.0` while changelog versions omit the
leading `v`.

## [Unreleased]

### Added

- Added a real Maven core-extension activation path so `.mvn/extensions.xml` can drive the common Maven lifecycle through Mill without a POM plugin declaration.
- Added end-to-end Maven fixture coverage for extension-only lifecycle forwarding, publish-capable `install` and `deploy`, multi-module overrides, and strict-failure diagnostics.

### Changed

- Strengthened the conventional Maven baseline so realistic Mill module queries cover the common lifecycle, while publish-related phases now explicitly depend on a `PublishModule`-capable Mill surface.

### Fixed

- Fixed Maven Mojo execution-context derivation to prefer real `MavenProject` and `MavenSession` state, which keeps module-local overrides working inside reactor builds.

### Documentation

- Documented real Maven plugin setup, minimal extension-only usage, config discovery, publish requirements, and override examples in the README and a dedicated usage guide.
- Documented the planned `0.4.0-rc.1` prerelease track and the follow-up documentation-site work required before the final `0.4.0` release.

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
