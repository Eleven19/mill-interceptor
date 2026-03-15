# Milli Launcher Redesign Design

## Goal

Redesign the `milli` and `milli.bat` launchers so they behave more like Mill's launcher while fitting this repository's current release and publishing model.

## Scope

This design covers:

- launcher version resolution
- launcher mode and source selection
- native-first execution with `dist` fallback
- Maven Central and GitHub Releases download behavior
- artifact naming updates from `assembly` to `dist`
- release and publish metadata changes required to support the new launcher behavior

This design does not cover:

- using the regular library jar from the launcher
- upward directory search for version files
- renaming the distributed binary or GitHub release asset family from `mill-interceptor` to `milli`

## Current State

The current `milli` launchers always download `milli-assembly` from Maven Central and run it with `java -jar`. The release and publish pipeline already emits:

- the regular library artifact
- `milli-assembly`
- platform-specific `milli-native-*` artifacts
- GitHub release assets for native binaries, the assembly jar, and the launcher scripts

There is no launcher-side selection logic for platform-native artifacts, release assets, source preference, or version files.

## Design Decisions

### Artifact Model

The regular library jar remains published to Maven Central but is not used by the launcher.

The executable assembly artifact is renamed from `milli-assembly` to `milli-dist`:

- Maven Central publishes `io.github.eleven19.mill-interceptor:milli-dist:<version>`
- GitHub Releases publish a `dist` jar asset instead of an `assembly` jar asset

Platform-native executables remain separate artifacts:

- `milli-native-linux-amd64`
- `milli-native-linux-aarch64`
- `milli-native-macos-amd64`
- `milli-native-macos-aarch64`
- `milli-native-windows-amd64`

### Version Resolution

The launcher resolves the version in this order:

1. `MILLI_VERSION`
2. `.mill-interceptor-version` in the current working directory
3. `.config/mill-interceptor-version` in the current working directory
4. embedded default release version

Version file lookup is local-directory only. The launcher does not walk parent directories.

### Launcher Mode

The launcher supports two execution modes:

- `native`
- `dist`

`MILLI_LAUNCHER_MODE` controls behavior:

- `native`: only use platform-native artifacts; fail if unsupported or unavailable
- `dist`: only use the `dist` jar; execute with `java -jar`
- unset: try `native` first, then `dist`

The launcher does not support a `jar` mode for the regular library artifact.

### Source Preference

`MILLI_LAUNCHER_SOURCE` selects the preferred artifact source:

- `maven`
- `github`

Default preference is `maven`.

The launcher always falls back to the secondary source if the preferred source does not provide the requested artifact.

### Authentication Behavior

By default, the launcher should not use `.netrc`.

`MILLI_LAUNCHER_USE_NETRC=1` enables `.netrc` support for `curl` or `wget` download calls when the active downloader supports it.

### Platform Mapping

The launcher maps the current platform to the existing native artifact family:

- Linux x86_64 -> `milli-native-linux-amd64`
- Linux aarch64 / arm64 -> `milli-native-linux-aarch64`
- macOS x86_64 -> `milli-native-macos-amd64`
- macOS arm64 -> `milli-native-macos-aarch64`
- Windows AMD64 -> `milli-native-windows-amd64`

Unsupported platforms skip native resolution in default mode and go directly to `dist`.

## Launcher Flow

### Default Flow

When `MILLI_LAUNCHER_MODE` is unset:

1. Resolve version from env, version file, or embedded default
2. Resolve preferred source from `MILLI_LAUNCHER_SOURCE`, defaulting to Maven Central
3. Detect whether the current platform has a supported native mapping
4. If native is supported:
   - try native from the preferred source
   - if that fails, try native from the fallback source
5. If native cannot be resolved, try `dist` from the preferred source
6. If that fails, try `dist` from the fallback source
7. Execute the resolved artifact

### Explicit Native Mode

When `MILLI_LAUNCHER_MODE=native`:

1. Resolve version and source preference
2. Require a supported native platform mapping
3. Try preferred source, then fallback source
4. If both fail, exit with a clear error

### Explicit Dist Mode

When `MILLI_LAUNCHER_MODE=dist`:

1. Resolve version and source preference
2. Try preferred source, then fallback source for the `dist` jar
3. Execute with `java -jar`
4. If both fail, exit with a clear error

## Download Sources

### Maven Central

Maven Central is the default source for both native artifacts and `dist`.

Expected artifact forms:

- `milli-dist-<version>.jar`
- `milli-native-<platform>-<version>.<ext>`

### GitHub Releases

GitHub Releases act as a fallback source for the same resolved version.

Expected release assets:

- platform-specific `mill-interceptor` native archives
- `dist` jar asset
- launcher scripts

The launcher should download only the asset needed for the chosen mode.

## Caching

The launcher should cache downloaded payloads by version and artifact kind under the existing launcher cache root.

Separate cache entries should exist for:

- each native platform artifact
- each `dist` jar version

This avoids collisions between mode fallbacks and source changes.

## Release and Build Changes

The build and release pipeline must align with the new launcher behavior:

- rename assembly release helpers to dist helpers
- rename assembly publish module and metadata to dist equivalents
- keep publishing the normal library jar as the root Maven artifact
- keep native publish modules as separate platform-specific artifacts
- update release workflow helper scripts and docs to use `dist`

## Failure Handling

The launcher should distinguish these cases clearly:

- unsupported platform in `native` mode
- artifact unavailable from preferred source
- artifact unavailable from both sources
- missing `java` when executing `dist`
- missing download tool (`curl`, `wget`, or Windows PowerShell path)

Default mode should degrade gracefully from native to dist. Explicit mode selection should fail fast once that mode is exhausted.

## Testing Strategy

Testing should cover:

- version precedence
- version file names and lookup order
- mode selection behavior
- source preference behavior
- URL and asset-name generation
- native-platform mapping
- `dist` artifact naming in publish metadata
- dry-run or rendered-script verification for both Unix and Windows launchers

The full Mill test suite should continue passing after the redesign.
