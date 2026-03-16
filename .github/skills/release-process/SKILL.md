---
name: release-process
description: Prepare and validate releases that publish curated changelog content plus generated git-cliff notes.
---

# Release Process

Use this skill for release workflow work, release docs, changelog promotion from
`[Unreleased]`, or changes that affect GitHub Release publication.

## Required checks

1. Ensure the target version exists in `CHANGELOG.md` as:
   - `## [1.2.3] - YYYY-MM-DD`
2. Keep `## [Unreleased]` present above the released section
3. Verify release-note generation locally before claiming success
4. Follow the release flow in `docs/contributing/releasing.md`

## References

- `docs/contributing/releasing.md`
- `docs/contributing/changelog.md`
- `CHANGELOG.md`
- `cliff.toml`
- `scripts/ci/build-release-notes.sh`
- `scripts/ci/recommend-prerelease.sh`

## Publish Next Prerelease

Use this when the user wants to cut the next prerelease but does not want to
manually derive the version.

1. Run `scripts/ci/recommend-prerelease.sh`.
2. Review the recommendation and alternatives:
   - recommended `rc`
   - `beta`
   - `alpha`
   - branch-based prerelease from the current branch name
   - patch/minor/major `rc` alternatives
3. Ask the user which channel or exact version to use.
4. Dispatch the existing release workflow with the chosen version:

```bash
gh workflow run release.yml --ref <branch> -f version=<chosen-version>
```

The recommendation logic uses the latest stable `vX.Y.Z` tag plus conventional
commit signals since that tag:

- breaking change -> next major prerelease
- `feat` -> next minor prerelease
- otherwise -> next patch prerelease

## Verification

Run:

```bash
scripts/ci/build-release-notes.sh 0.1.0
bash scripts/ci/test-build-release-notes.sh
```
