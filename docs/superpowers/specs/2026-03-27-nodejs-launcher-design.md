# Node.js milli.mjs Launcher Design

**Date:** 2026-03-27
**Issue:** MI-zxh — Add Node.js 24 milli.mjs launcher and publish it in release flows
**Status:** Approved

## Overview

Add a `milli.mjs` launcher alongside the existing `milli` (bash) and `milli.bat`
(Windows batch) scripts. The launcher targets Node.js 22+ (current LTS), is
zero-dependency (built-in modules only), and provides full cross-platform
feature parity with both existing launchers in a single file.

### Primary use cases

1. **Cross-platform single file** — one `milli.mjs` that works on Linux, macOS,
   and Windows, replacing the need to choose between `milli` and `milli.bat` in
   projects where Node.js is available.
2. **CI/toolchain integration** — for environments (containers, CI images,
   managed dev environments) where Node.js is guaranteed but bash may not be.

## Architecture

### File structure

`launcher/milli.mjs` is a single self-contained file with three logical
sections:

**1. Configuration constants** (top of file)

- Default version token (`@MILLI_DEFAULT_VERSION@`), version file paths,
  artifact names, GitHub release base URL, Maven Central URL patterns.
- Platform detection map: `os.platform()` + `os.arch()` mapped to native
  artifact name, release target, and archive extension.

**2. `MilliLauncher` class**

Constructor accepts an options object for dependency injection:

```js
{
  platform,       // os.platform()
  arch,           // os.arch()
  env,            // process.env
  cwd,            // process.cwd()
  fs,             // node:fs/promises + node:fs
  fetch,          // globalThis.fetch
  execFileSync,   // node:child_process.execFileSync
}
```

Defaults to real implementations from Node.js built-ins. Normal invocation is
`new MilliLauncher()` with no arguments.

Key methods:

| Method | Purpose |
|---|---|
| `resolveVersion()` | env var > version file > config file > default |
| `resolveMode()` | Validates and expands `auto` into ordered candidates |
| `resolveSourceOrder()` | maven-first or github-first ordering |
| `computeUrls(version)` | Returns dist + native URLs for both sources |
| `downloadFile(url, dest)` | fetch + write to temp file + rename |
| `extractArchive(path, dir)` | tar on Unix, tar/PowerShell on Windows |
| `ensureNative(source)` | Download + extract + chmod native binary |
| `ensureDist(source)` | Download dist jar |
| `dryRun(version)` | Print diagnostic output matching bash format |
| `run(args)` | Main entry point: resolve, download, exec |

**3. Entry point** (bottom of file)

```js
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const launcher = new MilliLauncher();
  await launcher.run(process.argv.slice(2));
}
```

The guard allows the file to be both imported (for tests) and executed directly.

### Invocation

- Shebang `#!/usr/bin/env node` for Unix convenience (`./milli.mjs`)
- Canonical cross-platform invocation: `node milli.mjs <args>`

## Platform Detection & Native Support

| `os.platform()` | `os.arch()` | Native Artifact | Release Target | Archive Ext |
|---|---|---|---|---|
| `linux` | `x64` | `milli-native-linux-amd64` | `x86_64-unknown-linux-gnu` | `tar.gz` |
| `linux` | `arm64` | `milli-native-linux-aarch64` | `aarch64-unknown-linux-gnu` | `tar.gz` |
| `darwin` | `x64` | `milli-native-macos-amd64` | `x86_64-apple-darwin` | `tar.gz` |
| `darwin` | `arm64` | `milli-native-macos-aarch64` | `aarch64-apple-darwin` | `tar.gz` |
| `win32` | `x64` | `milli-native-windows-amd64` | `x86_64-pc-windows-msvc` | `zip` |

Any other combination sets `nativeSupported = false` and falls back to
dist-jar only.

### Archive extraction

- **tar.gz**: `execFileSync('tar', ['-xzf', archivePath, '-C', extractDir])`
- **zip on Windows**: try `tar -xf` first (Windows 10+ ships tar.exe), fall
  back to `powershell -NoProfile -ExecutionPolicy Bypass -Command
  "Expand-Archive -Force ..."`

### Process execution

- **Native binary**: `execFileSync(nativePath, args, { stdio: 'inherit' })`
  with `process.exit(status)`
- **Dist jar**: `execFileSync('java', ['-jar', distPath, ...args],
  { stdio: 'inherit' })` with `process.exit(status)`

Unlike the bash launcher which uses `exec` (process replacement), Node.js
`execFileSync` spawns a child process. The exit code is propagated via
`process.exit()`.

## Version Resolution & Cache Layout

### Version resolution priority

1. `MILLI_VERSION` env var (highest)
2. `.mill-interceptor-version` file in cwd (first line, trimmed)
3. `.config/mill-interceptor-version` file in cwd
4. `@MILLI_DEFAULT_VERSION@` baked-in default (lowest)

### Cache directory

- `MILLI_CACHE_DIR` env var if set
- Platform default:
  - Unix: `${XDG_CACHE_HOME:-$HOME/.cache}/milli`
  - Windows: `${LOCALAPPDATA}/milli` (falling back to
    `${USERPROFILE}/.cache/milli`)

### Cache layout

```
<cache_root>/<version>/milli-dist-<version>.jar
<cache_root>/<version>/<native-artifact>/mill-interceptor[.exe]
<cache_root>/<version>/<native-artifact>-<version>.<ext>
```

### Environment variables

Full parity with existing launchers:

| Variable | Values | Default |
|---|---|---|
| `MILLI_VERSION` | any version string | (none) |
| `MILLI_CACHE_DIR` | directory path | platform default |
| `MILLI_LAUNCHER_MODE` | `auto`, `native`, `dist` | `auto` |
| `MILLI_LAUNCHER_SOURCE` | `maven`, `github` | `maven` |
| `MILLI_LAUNCHER_USE_NETRC` | `0`, `1` | `0` |
| `MILLI_LAUNCHER_DRY_RUN` | `0`, `1` | `0` |

### Netrc handling

When `MILLI_LAUNCHER_USE_NETRC=1`, the launcher parses `~/.netrc` (or
`%USERPROFILE%/_netrc` on Windows) and injects an `Authorization` header for
matching hosts into `fetch()` calls. This is additional work compared to the
bash launcher which delegates netrc to curl/wget.

## Dry Run Output

When `MILLI_LAUNCHER_DRY_RUN=1`, the launcher prints key=value diagnostic
lines matching the bash launcher's exact format, then exits 0:

```
version=<version>
mode=<mode>
mode_order=<comma-separated>
preferred_source=<source>
source_order=<comma-separated>
use_netrc=<0|1>
curl_netrc_flag=<--netrc or empty>
native_supported=<0|1>
native_artifact=<artifact>          # only if native_supported=1
native_release_target=<target>      # only if native_supported=1
native_maven_url=<url>              # only if native_supported=1
native_github_url=<url>             # only if native_supported=1
native_path=<path>                  # only if native_supported=1
dist_artifact=milli-dist
dist_maven_url=<url>
dist_github_url=<url>
dist_path=<path>
```

The `curl_netrc_flag` key is preserved for output compatibility even though the
Node.js launcher uses `fetch`.

## Build & Release Integration

### ReleaseSupport.scala changes

- `validatedLauncherOs`: add `"nodejs"` as a third accepted value
- `launcherFileNameFor`: return `"milli.mjs"` for `"nodejs"`

No other changes needed. The existing `releaseLauncher` task reads the
template, replaces the version token, and writes the output.

### Release workflow changes

Three new steps in the `extras` job of `.github/workflows/release.yml`, after
the Windows launcher steps:

1. **Compute Node.js launcher name** —
   `scripts/ci/compute-launcher-name.sh nodejs nodejs_launcher_name`
2. **Build Node.js launcher** —
   `scripts/ci/build-release-launcher.sh "$version" nodejs "$name"`
3. **Stage Node.js launcher** —
   `scripts/ci/stage-release-extra.sh "out/.../milli.mjs"
   release-extras/releaseLauncher.dest`

The existing CI scripts require no changes — they delegate to Mill tasks which
handle the new `nodejs` OS value via `ReleaseSupport.scala`.

### Release asset

`milli.mjs` is published alongside `milli` and `milli.bat` in the
`release-extras` artifact.

## Testing Strategy

### Unit tests — `launcher/milli.test.mjs`

Using Node.js built-in `node:test` and `node:assert`. Tests exercise
`MilliLauncher` with injected mock dependencies (no network, no filesystem
side effects):

- **Version resolution**: env var wins over file, file wins over config file,
  config file wins over default
- **Mode resolution**: `auto` expands to `[native, dist]` when native is
  supported, `[dist]` otherwise; `native`/`dist` produce single-element
  arrays; invalid mode rejects
- **Source ordering**: `maven` -> `[maven, github]`, `github` ->
  `[github, maven]`; invalid rejects
- **Platform detection**: each platform/arch combo maps to correct artifact
  name, release target, archive extension; unsupported combos yield
  `nativeSupported = false`
- **URL computation**: correct Maven Central and GitHub Release URLs for both
  dist and native artifacts
- **Cache path construction**: correct paths per platform (XDG on Unix,
  LOCALAPPDATA on Windows)
- **Dry run output**: format matches expected key=value output exactly
- **Netrc parsing**: correctly extracts credentials for matching hosts

### CI entry point — `scripts/ci/test-milli-mjs.sh`

A shell script that:

1. Checks `node --version` meets minimum (22+)
2. Runs `node --test launcher/milli.test.mjs`
3. Runs dry-run integration tests similar to `test-launchers.sh` — invokes
   `node launcher/milli.mjs` with `MILLI_LAUNCHER_DRY_RUN=1` and various
   env/file combinations, asserts output

### Existing test updates

- `scripts/ci/test-launchers.sh` — add `test -f launcher/milli.mjs` existence
  check
- `scripts/ci/test-release-workflows.sh` — add checks for the nodejs launcher
  steps in the release workflow
- `scripts/ci/test-release-extras-staging.sh` — add staging of `milli.mjs`
  alongside `milli` and `milli.bat`

## Deliverables

| # | File | Change |
|---|---|---|
| 1 | `launcher/milli.mjs` | New: cross-platform Node.js launcher |
| 2 | `launcher/milli.test.mjs` | New: unit tests |
| 3 | `scripts/ci/test-milli-mjs.sh` | New: CI test entry point |
| 4 | `mill-build/src/build/ReleaseSupport.scala` | Modify: add `nodejs` launcher OS |
| 5 | `.github/workflows/release.yml` | Modify: add Node.js launcher steps |
| 6 | `scripts/ci/test-launchers.sh` | Modify: add `milli.mjs` checks |
| 7 | `scripts/ci/test-release-workflows.sh` | Modify: add nodejs workflow checks |
| 8 | `scripts/ci/test-release-extras-staging.sh` | Modify: add `milli.mjs` staging |

## Key Decisions

- **Node.js 22+** (current LTS) for broad CI compatibility
- **Zero dependencies** — built-in modules only
- **Class-based** architecture for testability with dependency injection
- **Full native + dist** support on all platforms
- **Dry-run output** format matches bash launcher exactly
- **Third launcher** — added alongside unix/windows, not replacing either
- **Version token** — same `@MILLI_DEFAULT_VERSION@` mechanism as existing launchers
