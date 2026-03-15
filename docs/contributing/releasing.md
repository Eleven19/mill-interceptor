# Releasing Native Archives

This repository publishes GraalVM native executables through GitHub Releases as platform-specific archives that are compatible with `mise`'s GitHub backend.

## Supported targets

The release workflow currently targets:

- `x86_64-unknown-linux-gnu`
- `aarch64-unknown-linux-gnu`
- `x86_64-apple-darwin`
- `aarch64-apple-darwin`
- `x86_64-pc-windows-msvc`

## Versioning

Two release entrypoints are supported:

- Push a tag like `v1.2.3` or `v1.2.3-rc.1`
- Run `workflow_dispatch` and provide `version` as `1.2.3` or `1.2.3-rc.1`

The workflow normalizes the version by stripping any leading `v`, recreates the tag as `v<version>` when needed for manual releases, and marks the GitHub release as a prerelease when the semantic version contains a prerelease suffix.

## Artifact naming

Asset names follow the pattern:

- `mill-interceptor-v<version>-<target>.tar.gz` for Unix-like targets
- `mill-interceptor-v<version>-<target>.zip` for Windows

Examples:

- `mill-interceptor-v1.2.3-x86_64-unknown-linux-gnu.tar.gz`
- `mill-interceptor-v1.2.3-aarch64-apple-darwin.tar.gz`
- `mill-interceptor-v1.2.3-x86_64-pc-windows-msvc.zip`

Archive contents place the executable at the archive root:

- `mill-interceptor` on Unix-like targets
- `mill-interceptor.exe` on Windows

That layout keeps extraction simple for `mise`.

## Checksums

The publish job generates `SHA256SUMS` from the downloaded platform archives and uploads it to the GitHub release alongside the binaries.

## Manual release flow

To cut a manual prerelease or stable release:

1. Open the `Release` workflow in GitHub Actions.
2. Run `workflow_dispatch`.
3. Enter `version` without the leading `v`.

Examples:

- `1.2.3`
- `1.2.3-rc.1`

The workflow will:

1. Normalize the version and tag.
2. Build all supported platform archives.
3. Create or update the GitHub release.
4. Upload the archives and `SHA256SUMS`.
