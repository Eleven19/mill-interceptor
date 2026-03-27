# Release Artifacts

Releases publish both GitHub assets and Maven Central artifacts.

## GitHub release assets

GitHub Releases publish:

- platform-specific native archives
- executable assembly jar
- `milli`
- `milli.bat`

Stable releases use tags like `v1.2.3`. Prereleases use semantic prerelease tags like `v1.2.3-rc.1`.

## Maven Central artifacts

Maven Central publishes under `io.eleven19.mill-interceptor`:

- `milli`
- `milli-dist`
- platform-specific `milli-native-*` artifacts
- `mill-interceptor-maven-plugin`

## Launcher controls

Launchers support:

- `MILLI_VERSION`
- `.mill-interceptor-version`
- `.config/mill-interceptor-version`
- `MILLI_LAUNCHER_MODE` (`native` or `dist`)
- `MILLI_LAUNCHER_SOURCE` (`maven` or `github`)
- `MILLI_LAUNCHER_USE_NETRC=1`

For maintainer release procedures, see `docs/contributing/releasing.md`.
