# Namespace And Group Rename Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename the active Scala namespace to `io.eleven19.mill.interceptor`, rename the Maven group to `io.eleven19.mill-interceptor`, and align code, tests, release metadata, and active docs with those canonical names.

**Architecture:** Treat this as a full namespace migration rather than a publish-only tweak. First lock in the failing checks for package/group references that matter, then update central build metadata, move the source trees to match the new package layout, and finally update active docs plus a historical note in `docs/plans/`.

**Tech Stack:** Mill, Scala 3, Bash CI scripts, GitHub Actions, beads

---

### Task 1: Add Regression Checks For Namespace And Group References

**Files:**
- Modify: `scripts/ci/test-release-workflows.sh`
- Modify: `scripts/ci/test-publish-metadata.sh`
- Create: `scripts/ci/test-namespace-rename.sh`

**Step 1: Write the failing regression script**

Create `scripts/ci/test-namespace-rename.sh` with checks for:
- `build.mill.yaml` using `io.eleven19.mill.interceptor.Main`
- `mill-build/src/build/ReleaseSupport.scala` using `io.eleven19.mill-interceptor`
- `README.md` using `io.eleven19.mill-interceptor`
- no active `io.github.eleven19.mill.interceptor` references under:
  - `src/`
  - `test/`
  - `itest/`
  - `README.md`
  - `docs/contributing/`
  - `build.mill.yaml`
  - `mill-build/src/build/`
  - `scripts/ci/`

**Step 2: Run the regression script to verify it fails**

Run: `bash scripts/ci/test-namespace-rename.sh`
Expected: FAIL because the old namespace and group are still present.

**Step 3: Extend publish metadata checks**

Update `scripts/ci/test-publish-metadata.sh` so the expected coordinates use:
- `io.eleven19.mill-interceptor:milli`
- `io.eleven19.mill-interceptor:milli-dist`
- the platform-specific `milli-native-*` artifacts under the same group

**Step 4: Run the publish metadata check to verify it fails**

Run: `scripts/ci/test-publish-metadata.sh 0.1.0`
Expected: FAIL because the current build still publishes under `io.github.eleven19.mill-interceptor`.

**Step 5: Commit**

```bash
git add scripts/ci/test-namespace-rename.sh scripts/ci/test-publish-metadata.sh
git commit -m "Add namespace rename regression checks"
```

### Task 2: Rename Build Metadata And Runtime Identifiers

**Files:**
- Modify: `build.mill.yaml`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Modify: `src/io/github/eleven19/mill/interceptor/Main.scala`
- Modify: `src/io/github/eleven19/mill/interceptor/ScribeLog.scala`
- Modify: `src/io/github/eleven19/mill/interceptor/maven/MillTask.scala`
- Modify: `docs/contributing/logging-with-scribe.md`

**Step 1: Update the build metadata**

Change:
- `mainClass` to `io.eleven19.mill.interceptor.Main`
- Maven group constant to `io.eleven19.mill-interceptor`

**Step 2: Update runtime-facing strings**

Change logger/category strings and comments that embed:
- `io.github.eleven19.mill.interceptor`

to:
- `io.eleven19.mill.interceptor`

**Step 3: Run the new namespace regression test**

Run: `bash scripts/ci/test-namespace-rename.sh`
Expected: still FAIL because package declarations and file layout have not moved yet.

**Step 4: Commit**

```bash
git add build.mill.yaml mill-build/src/build/ReleaseSupport.scala src/io/github/eleven19/mill/interceptor/Main.scala src/io/github/eleven19/mill/interceptor/ScribeLog.scala src/io/github/eleven19/mill/interceptor/maven/MillTask.scala docs/contributing/logging-with-scribe.md
git commit -m "Rename build metadata and runtime identifiers"
```

### Task 3: Move Source Trees And Update Scala Packages

**Files:**
- Modify: all files under `src/io/github/eleven19/mill/interceptor/`
- Modify: all files under `test/src/io/github/eleven19/mill/interceptor/`
- Modify: all files under `itest/src/io/github/eleven19/mill/interceptor/`
- Modify: `itest/resources/junit-platform.properties`
- Create: matching files under `src/io/eleven19/mill/interceptor/`
- Create: matching files under `test/src/io/eleven19/mill/interceptor/`
- Create: matching files under `itest/src/io/eleven19/mill/interceptor/`
- Delete: the old `io/github/eleven19` trees after moving files

**Step 1: Move the production source tree**

Move the production files from:
- `src/io/github/eleven19/mill/interceptor/...`

to:
- `src/io/eleven19/mill/interceptor/...`

Then update package declarations and imports from:
- `io.github.eleven19.mill.interceptor...`

to:
- `io.eleven19.mill.interceptor...`

**Step 2: Move the test source tree**

Apply the same move and package/import updates under:
- `test/src/`

**Step 3: Move the integration-test source tree**

Apply the same move and package/import updates under:
- `itest/src/`

Also update:
- `itest/resources/junit-platform.properties`

so the `cucumber.glue` package list uses `io.eleven19...`.

**Step 4: Run targeted search to verify the active code tree is clean**

Run: `rg -n "io\\.github\\.eleven19\\.mill\\.interceptor" src test itest build.mill.yaml docs/contributing README.md mill-build/src/build scripts/ci`
Expected: no matches.

**Step 5: Run the regression script to verify it passes**

Run: `bash scripts/ci/test-namespace-rename.sh`
Expected: PASS.

**Step 6: Commit**

```bash
git add src test itest itest/resources/junit-platform.properties
git commit -m "Rename Scala packages to io.eleven19"
```

### Task 4: Update Active Docs And Plan-Directory Rename Note

**Files:**
- Modify: `README.md`
- Modify: `docs/contributing/releasing.md`
- Create: `docs/plans/2026-03-16-namespace-group-rename-note.md`

**Step 1: Update active user-facing docs**

Change active docs to use:
- `io.eleven19.mill-interceptor` for Maven coordinates
- `io.eleven19.mill.interceptor` for package/class references

Leave historical plan docs untouched.

**Step 2: Add the historical note under `docs/plans/`**

Create a short note explaining:
- older design docs may mention `io.github.eleven19...`
- the canonical replacements are now:
  - package base `io.eleven19.mill.interceptor`
  - Maven group `io.eleven19.mill-interceptor`

**Step 3: Run the namespace regression script again**

Run: `bash scripts/ci/test-namespace-rename.sh`
Expected: PASS.

**Step 4: Commit**

```bash
git add README.md docs/contributing/releasing.md docs/plans/2026-03-16-namespace-group-rename-note.md
git commit -m "Update docs for namespace rename"
```

### Task 5: Verify Publish And Release Metadata

**Files:**
- Modify: only if verification reveals missed references

**Step 1: Run publish metadata verification**

Run: `scripts/ci/test-publish-metadata.sh 0.1.0`
Expected: PASS with `io.eleven19.mill-interceptor` coordinates.

**Step 2: Run release workflow regression checks**

Run: `bash scripts/ci/test-release-workflows.sh`
Expected: PASS.

**Step 3: Run release artifact publish regression**

Run: `bash scripts/ci/test-release-artifact-publish.sh`
Expected: PASS.

**Step 4: If anything fails, apply the smallest fix and repeat**

Limit changes to the exact missed reference or metadata edge.

**Step 5: Commit any verification-driven fixes**

```bash
git add <fixed-files>
git commit -m "Fix remaining namespace metadata references"
```

### Task 6: Full Validation And Handoff

**Files:**
- Modify: `.beads/...` only through `bd`

**Step 1: Run the full test suite**

Run: `COURSIER_CACHE=/tmp/coursier ./mill --no-server test`
Expected: PASS.

**Step 2: Check for remaining active old references**

Run: `rg -n "io\\.github\\.eleven19(\\.mill\\.interceptor)?|io\\.github\\.eleven19\\.mill-interceptor" .`
Expected:
- matches only in preserved historical plan docs, if any
- no matches in active code, tests, build metadata, CI scripts, README, or contributing docs

**Step 3: Review git status**

Run: `git status --short`
Expected: clean working tree.

**Step 4: Close and sync the beads issue**

Run:

```bash
bd close MI-4r1 --reason "Completed"
bd dolt push
```

**Step 5: Push the branch**

Run:

```bash
git push -u origin mi-4r1-namespace-rename
```

**Step 6: Final handoff summary**

Report:
- the new canonical package and Maven group
- verification commands run
- any remaining out-of-scope blockers, especially the separate Central signing secret issue
