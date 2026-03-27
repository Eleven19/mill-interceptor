# Release and Maven Central Trigger Flow Design

## Goal

Ensure GitHub Releases and Maven Central publication follow the same release
trigger contract and can be started together in parallel for manual prereleases
and stable releases.

## Scope

This design covers:

- maintainer-facing release procedure updates
- a helper for dispatching both release workflows together
- updating prerelease recommendation output to point at the shared helper
- regression coverage for the shared manual release flow

This design does not cover:

- merging GitHub Release and Maven Central publication into one workflow
- changing the existing tag-push trigger model
- changing artifact publication logic inside either workflow

## Problem

The repository already has two independent workflows:

- `release.yml` for GitHub release assets
- `publish-central.yml` for Maven Central

They share the same tag-push trigger, but the documented manual prerelease flow
only dispatches `release.yml`. That makes branch-head prereleases incomplete by
default because Maven Central publication does not start unless a maintainer
dispatches it separately.

## Recommended Approach

Keep the workflows separate, but define a single shared manual dispatch entry
point in the repository:

- `scripts/ci/dispatch-release-suite.sh`

The helper should:

- accept a version and optional ref
- dispatch `release.yml`
- dispatch `publish-central.yml`
- use the same version/ref for both
- print the exact commands it is about to run

This preserves workflow isolation while making the maintainer procedure
consistent with the desired release contract: both workflows start from the same
trigger inputs and run in parallel.

## Maintainer Contract

### Tag-push releases

Tag pushes already satisfy the desired behavior:

- push `v<version>`
- both workflows start from the same tag independently

### Manual releases

Manual releases should no longer dispatch only `release.yml`.

Instead, maintainers should run the shared helper so both workflows start from
the same branch/ref and version:

```bash
scripts/ci/dispatch-release-suite.sh --ref main --version 1.2.3-rc.1
```

## Follow-up on Current RC

For `0.4.0-rc.1`, the GitHub Release already succeeded, but Maven Central did
not run because the old manual flow only started `release.yml`. After this
change lands, the current prerelease still needs a one-time manual dispatch of
`publish-central.yml` for `0.4.0-rc.1`.
