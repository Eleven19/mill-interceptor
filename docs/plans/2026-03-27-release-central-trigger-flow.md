# Release and Maven Central Trigger Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make manual prereleases and manual stable releases dispatch GitHub Release and Maven Central publication together in parallel with the same version/ref inputs.

**Architecture:** Leave `release.yml` and `publish-central.yml` as separate workflows, but add a repo-owned helper script that dispatches both with the same trigger contract. Update the release guide and prerelease recommendation helper to route maintainers through that shared entry point, and add shell-level regression tests for the helper and docs/workflow expectations.

**Tech Stack:** GitHub Actions, `gh`, Bash, release docs in `docs/contributing/releasing.md`, shell tests under `scripts/ci`

---

### Task 1: Add the shared manual dispatch helper

**Files:**
- Create: `scripts/ci/dispatch-release-suite.sh`
- Create: `scripts/ci/test-dispatch-release-suite.sh`
- Test: `bash scripts/ci/test-dispatch-release-suite.sh`

**Step 1: Write the helper script**

Add a helper that accepts:

- `--version <version>` required
- `--ref <ref>` optional, defaulting to the current branch

The script should dispatch:

- `gh workflow run release.yml --ref <ref> -f version=<version>`
- `gh workflow run publish-central.yml --ref <ref> -f version=<version>`

**Step 2: Add regression coverage**

Stub `gh` in a temp directory and assert the helper dispatches both workflows
with the same `--ref` and `version` values.

### Task 2: Route prerelease recommendations through the shared helper

**Files:**
- Modify: `scripts/ci/recommend-prerelease.sh`
- Modify: `scripts/ci/test-recommend-prerelease.sh`
- Test: `bash scripts/ci/test-recommend-prerelease.sh`

**Step 1: Update the recommended command**

Change the emitted dispatch command from a direct `gh workflow run release.yml`
call to the new shared helper so the recommendation reflects the canonical
manual flow.

**Step 2: Update the test**

Assert the JSON output contains the new helper-based dispatch command.

### Task 3: Update maintainer release documentation

**Files:**
- Modify: `docs/contributing/releasing.md`
- Modify: `CHANGELOG.md`
- Test: `rg -n "dispatch-release-suite|publish-central.yml|parallel|workflow_dispatch" docs/contributing/releasing.md CHANGELOG.md`

**Step 1: Update the release guide**

Document that:

- tag pushes already trigger both workflows from the same tag
- manual releases should use the shared helper
- the helper starts `release.yml` and `publish-central.yml` in parallel
- the `0.4.0-rc.1` gap came from the old manual process

**Step 2: Add a release-note-ready changelog entry**

Add concise `Documentation` and `CI` entries under `[Unreleased]`.

### Task 4: Extend workflow regression checks

**Files:**
- Modify: `scripts/ci/test-release-workflows.sh`
- Test: `bash scripts/ci/test-release-workflows.sh`

**Step 1: Add helper-script presence checks**

Assert the new helper script and its test exist.

**Step 2: Add content checks**

Assert the docs and recommendation helper reference the shared dispatch helper
and both workflow names.

### Task 5: Dispatch the missing Maven Central prerelease

**Files:**
- Modify: none
- Test: `gh run view <run-id> --json status,conclusion,jobs,url`

**Step 1: Use the shared dispatch pattern for the current gap**

Dispatch `publish-central.yml` for `0.4.0-rc.1` on `main`.

**Step 2: Monitor the run**

Confirm the workflow starts and report whether it succeeds.
