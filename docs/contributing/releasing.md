# Releasing GitHub Assets and Maven Artifacts

This repository publishes:

- GraalVM native executables through GitHub Releases as platform-specific archives compatible with `mise`
- an executable assembly jar through GitHub Releases
- `milli` and `milli.bat` launcher scripts through GitHub Releases
- the `milli` artifact family through Maven Central

## Overview

The release automation has four goals:

1. Build a native executable for each supported target with Mill and GraalVM native-image.
2. Package each executable into a predictable archive layout that `mise` can install from GitHub Releases.
3. Publish an executable assembly jar plus `milli` launcher scripts alongside the native archives in GitHub Releases.
4. Publish release notes that start with curated changelog content and then append generated `git-cliff` notes.
5. Publish the `milli`, the `milli-dist` assembly jar artifact, and platform-specific `milli-native-*` artifacts to Maven Central from a separate workflow.

The workflow definitions live in:

- `.github/workflows/release.yml` for GitHub release assets
- `.github/workflows/publish-central.yml` for Maven Central

The workflow shell logic is intentionally kept in `scripts/ci/` so the YAML stays declarative and the release steps can be tested locally.

## Release flow diagram

```text
             +----------------------+
             |  tag push: v1.2.3    |
             |         or           |
             | workflow_dispatch    |
             | version=1.2.3-rc.1   |
             +----------+-----------+
                        |
                        v
             +----------------------+
             | metadata job         |
             | - normalize version  |
             | - compute tag        |
             | - mark prerelease    |
             +----------+-----------+
                        |
                        v
             +----------------------+
             | build matrix job     |
             | per target:          |
             | - resolve asset name |
             | - build native image |
             | - package archive    |
             | - upload artifact    |
             +----------+-----------+
                        |
                        v
             +----------------------+
             | extras job           |
             | - build assembly jar |
             | - stage milli        |
             | - stage milli.bat    |
             | - upload artifacts   |
             +----------+-----------+
                        |
                        v
             +----------------------+
             | publish job          |
             | - create tag if      |
             |   workflow_dispatch  |
             | - validate changelog |
             | - build release body |
             | - download artifacts |
             | - generate checksums |
             | - create/update      |
             |   GitHub release     |
             | - upload assets      |
             +----------------------+
```

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

## Recommending the next prerelease

When you want to cut a prerelease but do not want to derive the version
manually, use:

```bash
scripts/ci/recommend-prerelease.sh
```

The helper:

- finds the latest stable `vX.Y.Z` tag
- inspects commits since that tag using conventional-commit signals
- recommends the next prerelease base:
  - breaking change -> next major
  - `feat` -> next minor
  - otherwise -> next patch
- computes the next prerelease number for:
  - `rc`
  - `beta`
  - `alpha`
  - a branch-based prerelease identifier derived from the current branch name

Example output:

```text
Latest stable tag: v0.1.0
Current branch: mi-123-launcher-redesign
Branch prerelease channel: mi-123-launcher-redesign

Recommended prerelease:
- rc: 0.2.0-rc.1

Alternative channels on 0.2.0:
- beta: 0.2.0-beta.1
- alpha: 0.2.0-alpha.1
- branch: 0.2.0-mi-123-launcher-redesign.1
```

After reviewing the recommendation, ask the user which channel or exact version
to use and dispatch the existing release workflow:

```bash
gh workflow run release.yml --ref <branch> -f version=<chosen-version>
```

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

### GitHub Release workflow

The release workflow has four jobs.

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

```text
input version/tag
      |
      v
+--------------------+
| strip leading `v`  |
+--------------------+
      |
      v
+--------------------+
| validate semver    |
+--------------------+
      |
      v
+--------------------+      +----------------------+
| prerelease suffix? |----->| prerelease=true      |
+--------------------+ yes  +----------------------+
      |
      no
      v
+----------------------+
| prerelease=false     |
+----------------------+
      |
      v
+----------------------+
| emit version/tag     |
+----------------------+
```

### 2. `build`

This job runs as a matrix over the supported targets. For each target it:

- Resolves the final archive name through Mill
- Builds the native image and packages it into a release archive
- Uploads the resulting archive as a workflow artifact

The build job delegates shell logic to:

- `scripts/ci/compute-archive-name.sh`
- `scripts/ci/build-release-archive.sh`

### 3. `extras`

This job runs once on Linux and produces the non-native release assets:

- executable assembly jar
- `milli`
- `milli.bat`

It delegates shell logic to:

- `scripts/ci/compute-assembly-name.sh`
- `scripts/ci/compute-launcher-name.sh`
- `scripts/ci/build-release-assembly.sh`
- `scripts/ci/build-release-launcher.sh`

### 4. `publish`

This job only runs after the native matrix and extras jobs succeed. It:

- Creates the tag for `workflow_dispatch` releases if it does not already exist
- Installs `git-cliff`
- Validates that `CHANGELOG.md` contains the exact version section
- Builds a release body with curated changelog content first and generated notes second
- Downloads the matrix artifacts
- Generates `SHA256SUMS`
- Creates or updates the GitHub release
- Uploads the platform archives and checksums

The publish job delegates shell logic to:

- `scripts/ci/create-release-tag.sh`
- `scripts/ci/install-git-cliff.sh`
- `scripts/ci/build-release-notes.sh`
- `scripts/ci/generate-checksums.sh`
- `scripts/ci/create-or-update-github-release.sh`
- `scripts/ci/upload-release-assets.sh`

```text
workflow_dispatch? ---- yes ----> create missing tag v<version>
        | no
        v
validate exact CHANGELOG section
        |
        v
build hybrid release body
        |
        v
download build artifacts
        |
        v
generate SHA256SUMS
        |
        v
release exists? ---- yes ----> update release metadata
        | no
        v
create release
        |
        v
upload archives + SHA256SUMS
```

## Changelog and release notes contract

The checked-in `CHANGELOG.md` is the curated source of truth for release
summaries.

For a release like `v1.2.3`, the workflow requires an exact changelog section:

```markdown
## [1.2.3] - 2026-03-14
```

If that section does not exist, the publish job fails before it creates or
updates the GitHub Release.

The final GitHub Release body is assembled as:

1. Curated changelog section for the exact version
2. Divider
3. Generated `git-cliff` notes for the same repository state

```text
         +----------------------------+
         | CHANGELOG.md exact        |
         | version section           |
         +-------------+--------------+
                       |
                       v
         +----------------------------+
         | scripts/ci/build-         |
         | release-notes.sh          |
         +------+------+-------------+
                |      |
                |      v
                |  +------------------+
                |  | git-cliff notes  |
                |  +------------------+
                |      |
                +------+
                       |
                       v
         +----------------------------+
         | final GitHub Release body  |
         +----------------------------+
```

## Artifact contract

Asset names follow the pattern:

- `mill-interceptor-v<version>-<target>.tar.gz` for Unix-like targets
- `mill-interceptor-v<version>-<target>.zip` for Windows

Examples:

- `mill-interceptor-v1.2.3-x86_64-unknown-linux-gnu.tar.gz`
- `mill-interceptor-v1.2.3-aarch64-apple-darwin.tar.gz`
- `mill-interceptor-v1.2.3-x86_64-pc-windows-msvc.zip`
- `mill-interceptor-dist-v1.2.3.jar`
- `milli`
- `milli.bat`

Archive contents place the executable at the archive root:

- `mill-interceptor` on Unix-like targets
- `mill-interceptor.exe` on Windows

That layout keeps extraction simple for `mise`.

## Mill release commands

The build exposes release-specific commands through `mill-build/src/build/ReleaseSupport.scala`.

Useful local commands:

```bash
./mill show modules.mill-interceptor.releaseTargets
./mill show modules.mill-interceptor.releaseAssetName --version 1.2.3 --target x86_64-unknown-linux-gnu
./mill show modules.mill-interceptor.releaseArchive --version 1.2.3 --target x86_64-unknown-linux-gnu
./mill show modules.mill-interceptor.releaseAssemblyAssetName --version 1.2.3
./mill show modules.mill-interceptor.releaseAssembly --version 1.2.3
./mill show modules.mill-interceptor.releaseLauncher --version 1.2.3 --launcher-os unix
./mill show modules.mill-interceptor.publishArtifactSummary
```

These commands are what the workflow uses under the hood. If you need to debug packaging behavior, start here rather than editing the workflow first.

## Packaging flow diagram

```text
modules.mill-interceptor.releaseArchive(version, target)
          |
          v
+---------------------------+
| validate target triple    |
+---------------------------+
          |
          v
+---------------------------+
| run Mill nativeImage      |
+---------------------------+
          |
          v
+---------------------------+
| stage executable as       |
| mill-interceptor(.exe)    |
+---------------------------+
          |
          v
   +------+------+
   | target OS?  |
   +------+------+
          |
   +------+------+
   |             |
 unix          windows
   |             |
   v             v
tar.gz         zip
   |             |
   +------+------+
          |
          v
+---------------------------+
| emit final release asset  |
+---------------------------+
```

## Local verification

Before pushing release-workflow changes, run at least:

```bash
COURSIER_CACHE=/tmp/coursier ./mill modules.mill-interceptor.test.testLocal
COURSIER_CACHE=/tmp/coursier ./mill show modules.mill-interceptor.releaseTargets
COURSIER_CACHE=/tmp/coursier ./mill show modules.mill-interceptor.releaseArchive --version 1.2.3 --target aarch64-unknown-linux-gnu
scripts/verify-release-archive.sh \
  out/modules/mill-interceptor/releaseArchive.dest/mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz \
  mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz \
  mill-interceptor
```

For workflow-script verification, useful spot checks are:

```bash
RUNNER_TEMP=/tmp scripts/ci/compute-archive-name.sh 1.2.3 aarch64-unknown-linux-gnu
RUNNER_TEMP=/tmp scripts/ci/build-release-archive.sh 1.2.3 aarch64-unknown-linux-gnu mill-interceptor-v1.2.3-aarch64-unknown-linux-gnu.tar.gz
RUNNER_TEMP=/tmp scripts/ci/compute-assembly-name.sh 1.2.3
RUNNER_TEMP=/tmp scripts/ci/build-release-assembly.sh 1.2.3 mill-interceptor-dist-v1.2.3.jar
RUNNER_TEMP=/tmp scripts/ci/compute-launcher-name.sh unix unix_launcher_name
RUNNER_TEMP=/tmp scripts/ci/build-release-launcher.sh 1.2.3 unix milli
scripts/ci/test-publish-metadata.sh 1.2.3
RUNNER_TEMP=/tmp scripts/ci/install-git-cliff.sh 2.12.0
scripts/ci/build-release-notes.sh 0.1.0
bash scripts/ci/test-build-release-notes.sh
```

## Maven Central workflow

The Maven Central workflow is intentionally separate from GitHub Releases so publication failures do not block release asset publication.

`publish-central.yml`:

- triggers on `v*` tags
- normalizes the release version with the same metadata action used by `release.yml`
- publishes `milli` and the `milli-dist` assembly jar artifact on Linux
- publishes each platform-specific `milli-native-*` artifact on its matching runner

The workflow expects Actions secrets for:

- `ELEVEN19_SONATYPE_USERNAME`
- `ELEVEN19_SONATYPE_PASSWORD`
- `ELEVEN19_IO_PGP_SECRET_BASE64`
- `ELEVEN19_IO_PGP_PASSPHRASE`

These are organization-provided secrets already used by other Eleven19 repositories. The workflow forwards the Sonatype credentials and base64-encoded PGP material directly to Mill rather than importing a raw GPG key inside Actions.

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
3. Validate the exact changelog section for that version.
4. Build all supported platform archives.
5. Generate `SHA256SUMS`.
6. Build the hybrid release notes body.
7. Create or update the GitHub release.
8. Upload the archives and checksums.

## Tag-driven stable release flow

To cut a stable release from a tag:

```bash
git tag v1.2.3
git push origin v1.2.3
```

That path skips manual tag creation because the pushed tag is already the source of truth for the release.

## Missing changelog section

If a release fails with a missing-version changelog error, fix `CHANGELOG.md`
before retrying. The workflow intentionally refuses to fall back to generated
notes only.

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
./mill show modules.mill-interceptor.releaseAssetName --version 1.2.3 --target x86_64-pc-windows-msvc
```

### Workflow YAML is getting hard to read

Put shell logic in `scripts/ci/` and keep the workflow file focused on orchestration.
