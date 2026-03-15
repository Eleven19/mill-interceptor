# Releasing Native Archives

This repository publishes GraalVM native executables through GitHub Releases as platform-specific archives that are compatible with `mise`'s GitHub backend.

## Overview

The release pipeline has three goals:

1. Build a native executable for each supported target with Mill and GraalVM native-image.
2. Package each executable into a predictable archive layout that `mise` can install from GitHub Releases.
3. Publish all archives plus `SHA256SUMS` to a single GitHub release driven by either a pushed tag or a manual workflow dispatch.

The workflow definition lives in `.github/workflows/release.yml`. The workflow shell logic is intentionally kept in `scripts/ci/` so the YAML stays declarative and the release steps can be tested locally.

## Supported targets

The release workflow currently targets:

- `x86_64-unknown-linux-gnu`
- `aarch64-unknown-linux-gnu`
- `x86_64-apple-darwin`
- `aarch64-apple-darwin`
- `x86_64-pc-windows-msvc`

These match the native executable platforms Mill currently documents as supported.

## Release entrypoints

Two release entrypoints are supported:

- Push a tag like `v1.2.3` or `v1.2.3-rc.1`
- Run `workflow_dispatch` and provide `version` as `1.2.3` or `1.2.3-rc.1`

Tag pushes are the normal stable-release path. `workflow_dispatch` is useful for prereleases, dry-run-style cuts against a branch head, or when you want the workflow to create the matching Git tag for you.

## Versioning rules

The repository uses two related but different version forms:

- Git tags always include a leading `v`
- Internal workflow version values do not include the leading `v`

Examples:

- Tag: `v1.2.3`
- Version input: `1.2.3`
- Tag: `v1.2.3-rc.1`
- Version input: `1.2.3-rc.1`

The release metadata action normalizes the version by stripping any leading `v`, rebuilding the tag as `v<version>`, and detecting prereleases from semantic version syntax.

## Workflow structure

The release workflow has three jobs.

### 1. `metadata`

This job:

- Reads the version from either the pushed tag or `workflow_dispatch`
- Validates semantic version syntax
- Computes:
  - normalized `version`
  - `tag`
  - `prerelease`
  - `release_name`

The normalization logic lives in `.github/actions/release-metadata/`.

### 2. `build`

This job runs as a matrix over the supported targets. For each target it:

- Resolves the final archive name through Mill
- Builds the native image and packages it into a release archive
- Uploads the resulting archive as a workflow artifact

The build job delegates shell logic to:

- `scripts/ci/compute-archive-name.sh`
- `scripts/ci/build-release-archive.sh`

### 3. `publish`

This job only runs after all matrix builds succeed. It:

- Creates the tag for `workflow_dispatch` releases if it does not already exist
- Downloads the matrix artifacts
- Generates `SHA256SUMS`
- Creates or updates the GitHub release
- Uploads the platform archives and checksums

The publish job delegates shell logic to:

- `scripts/ci/create-release-tag.sh`
- `scripts/ci/generate-checksums.sh`
- `scripts/ci/create-or-update-github-release.sh`
- `scripts/ci/upload-release-assets.sh`

## Artifact contract

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

## Mill release commands

The build exposes release-specific commands through `mill-build/src/build/ReleaseSupport.scala`.

Useful local commands:

```bash
./mill show releaseTargets
./mill show releaseAssetName --version 1.2.3 --target x86_64-unknown-linux-gnu
./mill show releaseArchive --version 1.2.3 --target x86_64-unknown-linux-gnu
```

These commands are what the workflow uses under the hood. If you need to debug packaging behavior, start here rather than editing the workflow first.

## Local verification

Before pushing release-workflow changes, run at least:

```bash
COURSIER_CACHE=/tmp/coursier ./mill test.testLocal
COURSIER_CACHE=/tmp/coursier ./mill show releaseTargets
COURSIER_CACHE=/tmp/coursier ./mill show releaseArchive --version 1.2.3 --target aarch64-unknown-linux-gnu
scripts/verify-release-archive.sh \
  out/releaseArchive.dest/mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz \
  mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz \
  mill-interceptor
```

For workflow-script verification, useful spot checks are:

```bash
RUNNER_TEMP=/tmp scripts/ci/compute-archive-name.sh 1.2.3 aarch64-unknown-linux-gnu
RUNNER_TEMP=/tmp scripts/ci/build-release-archive.sh 1.2.3 aarch64-unknown-linux-gnu mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz
```

## Manual release flow

To cut a manual prerelease or stable release:

1. Open the `Release` workflow in GitHub Actions.
2. Run `workflow_dispatch`.
3. Enter `version` without the leading `v`.

Examples:

- `1.2.3`
- `1.2.3-rc.1`

The workflow will:

1. Normalize the version and prerelease state.
2. Ensure the corresponding tag is `v<version>`.
3. Build all supported platform archives.
4. Generate `SHA256SUMS`.
5. Create or update the GitHub release.
6. Upload the archives and checksums.

## Tag-driven stable release flow

To cut a stable release from a tag:

```bash
git tag v1.2.3
git push origin v1.2.3
```

That path skips manual tag creation because the pushed tag is already the source of truth for the release.

## Checksums

The publish job generates `SHA256SUMS` from the downloaded platform archives and uploads it to the GitHub release alongside the binaries.

To verify a downloaded archive locally:

```bash
sha256sum -c SHA256SUMS --ignore-missing
```

## `mise` expectations

The current release contract is designed for `mise`'s GitHub backend:

- asset names encode version and target
- archive types are conventional for each platform
- the executable is at archive root

If you change the asset naming pattern or move the executable into nested directories, verify `mise` installation behavior before shipping the change.

## Troubleshooting

### Native image builds fail on one matrix target

Start by checking whether the failure is target-specific or shared:

- If only one runner fails, inspect that matrix job’s native-image output first.
- If every target fails in the same place, inspect the Mill command or metadata logic instead.

### Workflow dispatch created the wrong release type

Check the `metadata` job output and confirm the version follows semantic versioning:

- `1.2.3` -> stable release
- `1.2.3-rc.1` -> prerelease

### Archive naming looks wrong

Verify locally with:

```bash
./mill show releaseAssetName --version 1.2.3 --target x86_64-pc-windows-msvc
```

### Workflow YAML is getting hard to read

Put shell logic in `scripts/ci/` and keep the workflow file focused on orchestration.
