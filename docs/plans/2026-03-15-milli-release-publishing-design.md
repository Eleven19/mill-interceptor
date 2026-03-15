# Milli Release Publishing Design

## Goal

Extend the repository's release story in three directions:

- add an executable assembly jar to GitHub Releases
- publish artifacts to Maven Central under `io.github.eleven19.mill-interceptor`
- introduce `milli` and `milli.bat` launchers that acquire the published assembly artifact

## Scope

This design covers:

- GitHub Release publication of the executable assembly jar alongside existing native archives
- Maven Central publication for the regular library artifact, executable assembly artifact, and platform-specific native artifacts
- artifact naming and coordinate structure for a new `milli` artifact family
- launcher scripts that download and run the published assembly artifact
- workflow separation between GitHub Release publication and Maven Central publication

This design does not cover:

- renaming the existing native executable or GitHub release asset family from `mill-interceptor` to `milli`
- snapshot publication implementation for pull requests
- installer UX beyond `milli` and `milli.bat`

Follow-up analysis for renaming native assets and binaries is tracked separately in `MI-rrx`.

## Constraints

- The repository already publishes platform-specific native archives through [`.github/workflows/release.yml`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/.github/workflows/release.yml).
- Native archive naming and packaging currently live in [`mill-build/src/build/ReleaseSupport.scala`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/mill-build/src/build/ReleaseSupport.scala) and the shell wrappers in [`scripts/ci/`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/scripts/ci).
- Git tags use `v<version>`, while internal publish logic should use `<version>`.
- Maven Central publication must support stable and prerelease tags such as `v1.2.3-rc.1`.
- GitHub organization secrets already exist for publication and should be consumed by GitHub Actions rather than inventing a repo-local secret scheme.

## Recommended Approach

Keep GitHub Release distribution and Maven Central publication as separate release paths, with the Mill build defining artifact metadata and GitHub Actions providing orchestration.

The release workflow should remain responsible for native distribution assets and gain one additional asset: an executable assembly jar. A separate `publish-central` workflow should publish the Maven coordinates for the new `milli` artifact family. This separation keeps retries and failure modes clean while still using the same version normalization rules.

The published Central identity should follow Mill's multi-artifact style:

- one primary library artifact
- one assembly artifact
- one native artifact per platform

Rather than renaming the existing GitHub release assets immediately, Central publication and launcher UX should move forward first, and the rename analysis should remain deferred to `MI-rrx`.

## Artifact Model

### GitHub Release Assets

GitHub Releases should continue shipping the existing native archives:

- `mill-interceptor-v<version>-x86_64-unknown-linux-gnu.tar.gz`
- `mill-interceptor-v<version>-aarch64-unknown-linux-gnu.tar.gz`
- `mill-interceptor-v<version>-x86_64-apple-darwin.tar.gz`
- `mill-interceptor-v<version>-aarch64-apple-darwin.tar.gz`
- `mill-interceptor-v<version>-x86_64-pc-windows-msvc.zip`

The release should also add:

- an executable assembly jar asset
- `milli`
- `milli.bat`
- `SHA256SUMS`

The native asset names and binary names remain unchanged in this task.

### Maven Central Coordinates

Use group:

- `io.github.eleven19.mill-interceptor`

Use the `milli` artifact family:

- `milli` for the regular library artifact
- `milli-assembly` for the executable assembly jar
- one artifact ID per platform-native executable

Recommended native artifact IDs:

- `milli-native-linux-amd64`
- `milli-native-linux-aarch64`
- `milli-native-macos-amd64`
- `milli-native-macos-aarch64`
- `milli-native-windows-amd64`

These Central artifact IDs should use concise OS and architecture names, while GitHub Release assets continue using full target triples for compatibility with the existing release contract.

### Launcher Scripts

`milli` and `milli.bat` should act as thin launchers that acquire the `milli-assembly` artifact from Maven Central and execute it locally. They should be published as release assets and checked into the repository so they can be versioned and tested.

The launcher scripts should not depend on GitHub Releases for artifact acquisition. Their source of truth should be Maven Central so they line up with the published `milli` artifact family.

## Workflow Design

### 1. Existing `release.yml`

Keep the current release workflow structure:

- `metadata`
- `build`
- `publish`

Extend it so that:

- the build job also produces the executable assembly jar and launcher assets
- the publish job uploads native archives, assembly jar, launcher scripts, and checksums

The workflow should continue supporting:

- pushed version tags like `v1.2.3`
- manual `workflow_dispatch` releases with a normalized version input

### 2. New `publish-central.yml`

Add a separate workflow triggered by version tags like:

- `v1.2.3`
- `v1.2.3-rc.1`

This workflow should:

- normalize the tag to a publish version without the leading `v`
- configure Java and Mill
- load organization-provided publication and signing secrets
- publish the complete `milli` artifact set to Maven Central

The workflow should be structured so that a future pull-request snapshot path can be added without changing the core artifact model.

## Mill Build Changes

The Mill build should grow from pure release-archive support into a broader publication surface.

Add tasks or modules that can:

- produce the executable assembly jar with a deterministic output path and asset name
- describe the Central publication artifact list for the `milli` family
- build or stage the launcher scripts
- keep native release asset naming logic centralized in the build rather than duplicated across shell scripts and workflows

The existing [`ReleaseSupport.scala`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/mill-build/src/build/ReleaseSupport.scala) trait is the natural starting point, but it should expand beyond native archives into a more complete release-support surface.

## Version Semantics

Use one normalized version model across both workflows:

- Git tag: `v1.2.3`
- Publish version: `1.2.3`

Rules:

- stable tags publish to GitHub Releases and Maven Central
- prerelease tags such as `v1.2.3-rc.1` publish to GitHub Releases and Maven Central
- snapshot publishing is not implemented in this task, but the workflow and artifact naming should leave room for a future `-SNAPSHOT` path on pull requests

## Credentials and Signing

The new Central workflow should consume GitHub organization secrets already available to Actions. Secret names should be wired through workflow environment variables so the Mill build remains independent of GitHub-specific secret naming.

Missing-secret failures should be explicit and early. The workflow should fail before attempting publication if required credentials or signing material are absent.

## Testing Strategy

Add local verification for:

- release asset naming
- assembly jar generation
- publish-artifact enumeration
- launcher script generation
- launcher script basic execution contract

Add CI verification for:

- the existing test suite
- release workflow generation of native archives plus assembly asset
- Central workflow metadata and publish wiring

Where possible, keep verification in shell scripts under [`scripts/ci/`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/scripts/ci) so workflow logic remains testable outside GitHub Actions.

## Risks and Mitigations

### Coupled publishing failures

Risk:
GitHub Release and Maven Central failures become hard to reason about if one workflow owns both.

Mitigation:
Keep publication in two workflows with shared version normalization but independent retries.

### Artifact identity drift

Risk:
GitHub release assets and Maven Central coordinates could diverge in inconsistent ways.

Mitigation:
Use build-owned metadata and naming helpers for both workflows, and defer binary rename work to `MI-rrx` instead of partially renaming now.

### Launcher fragility

Risk:
Launchers can become brittle if they depend on a non-deterministic download source or undocumented local cache behavior.

Mitigation:
Make Maven Central the only launcher acquisition source, version the scripts in-repo, and add script-level tests for resolution and invocation behavior.

## References

- [`.github/workflows/release.yml`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/.github/workflows/release.yml)
- [`mill-build/src/build/ReleaseSupport.scala`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/mill-build/src/build/ReleaseSupport.scala)
- [`docs/contributing/releasing.md`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-615-release-publishing/docs/contributing/releasing.md)
- [Mill publishing documentation](https://mill-build.org/mill/javalib/publishing.html)
- [Mill installation documentation](https://mill-build.org/mill/1.0.x/cli/installation-ide.html)
