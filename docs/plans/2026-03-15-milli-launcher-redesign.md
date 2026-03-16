# Milli Launcher Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign `milli` and `milli.bat` to prefer native artifacts with `dist` fallback, add version/source controls, and align release metadata with the new `dist` artifact family.

**Architecture:** Keep the launcher as shell and batch scripts, but move artifact naming consistency into the Mill build and release helpers. Publish the regular library jar as before, rename the executable assembly artifact family to `dist`, and add deterministic launcher resolution logic for version, mode, source preference, and fallback.

**Tech Stack:** Mill, Scala 3, Bash, Windows batch, GitHub Actions

---

### Task 1: Rename the Executable Assembly Artifact Family to `dist`

**Files:**
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Modify: `mill-build/src/build/PublishSupport.scala`
- Modify: `scripts/ci/test-publish-metadata.sh`

**Step 1: Write the failing metadata checks**

Update `scripts/ci/test-publish-metadata.sh` so it expects:

- `io.github.eleven19.mill-interceptor:milli-dist:<version>`
- a `distPublish` module payload
- a `milli-dist-<version>.jar` filename

**Step 2: Run the metadata test to verify it fails**

Run:

```bash
scripts/ci/test-publish-metadata.sh 1.2.3
```

Expected: failure because the build still exposes `milli-assembly` and `assemblyPublish`.

**Step 3: Implement the build metadata rename**

Update release and publish support so:

- `publishAssemblyArtifact` becomes `publishDistArtifact`
- `assemblyAssetNameFor` becomes a dist naming helper
- `releaseAssemblyAssetName` / `releaseAssembly` become dist equivalents
- `assemblyPublish` becomes `distPublish`
- publish summary and artifact payloads use `milli-dist`

**Step 4: Run the metadata test to verify it passes**

Run:

```bash
scripts/ci/test-publish-metadata.sh 1.2.3
```

Expected: pass with `milli-dist` and `distPublish`.

**Step 5: Commit**

```bash
git add mill-build/src/build/ReleaseSupport.scala mill-build/src/build/PublishSupport.scala scripts/ci/test-publish-metadata.sh
git commit -m "build: rename launcher assembly artifacts to dist"
```

### Task 2: Add Launcher Dry-Run Coverage for Version and Source Resolution

**Files:**
- Modify: `launcher/milli`
- Modify: `launcher/milli.bat`
- Modify: `scripts/ci/test-launchers.sh`

**Step 1: Write the failing launcher tests**

Extend `scripts/ci/test-launchers.sh` so it verifies the rendered launchers include:

- `.mill-interceptor-version`
- `.config/mill-interceptor-version`
- `MILLI_LAUNCHER_MODE`
- `MILLI_LAUNCHER_SOURCE`
- `MILLI_LAUNCHER_USE_NETRC`
- `milli-dist`
- GitHub Releases fallback references

Prefer text assertions and dry-run hooks rather than real downloads.

**Step 2: Run the launcher test to verify it fails**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: failure because current launchers only mention `MILLI_VERSION` and `milli-assembly`.

**Step 3: Add dry-run support hooks**

Add simple dry-run behavior to both launchers so tests can inspect:

- resolved version
- chosen mode
- chosen source order
- selected artifact name
- selected download URL

without performing downloads or execution.

**Step 4: Re-run the launcher test to verify the dry-run hooks are visible**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: pass for the new env vars and `dist` naming.

**Step 5: Commit**

```bash
git add launcher/milli launcher/milli.bat scripts/ci/test-launchers.sh
git commit -m "test: add launcher dry-run coverage"
```

### Task 3: Implement Unix Launcher Native-First Resolution

**Files:**
- Modify: `launcher/milli`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Test: `scripts/ci/test-launchers.sh`

**Step 1: Add failing Unix launcher checks**

Expand `scripts/ci/test-launchers.sh` to validate the Unix launcher resolves:

- version precedence: env, local version file, `.config` version file, embedded default
- default mode order: native then dist
- default source order: Maven then GitHub
- explicit `native` and `dist` behavior
- `.netrc` opt-in wiring

Use dry-run invocations with environment variables and temporary directories.

**Step 2: Run the launcher test and verify Unix cases fail**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: Unix dry-run cases fail until the script is updated.

**Step 3: Implement Unix launcher resolution**

Update `launcher/milli` to:

- read `.mill-interceptor-version` and `.config/mill-interceptor-version`
- honor `MILLI_LAUNCHER_MODE`
- honor `MILLI_LAUNCHER_SOURCE`
- honor `MILLI_LAUNCHER_USE_NETRC`
- map supported Unix platforms to native artifact IDs and release assets
- attempt native then dist in default mode
- fall back from Maven to GitHub automatically
- emit clear errors for explicit mode failures

If needed, add small release helper tasks in `ReleaseSupport.scala` to expose stable `dist` asset names for script generation and tests.

**Step 4: Re-run launcher tests**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: Unix launcher cases pass.

**Step 5: Commit**

```bash
git add launcher/milli mill-build/src/build/ReleaseSupport.scala scripts/ci/test-launchers.sh
git commit -m "feat: add unix milli launcher resolution logic"
```

### Task 4: Implement Windows Launcher Native-First Resolution

**Files:**
- Modify: `launcher/milli.bat`
- Test: `scripts/ci/test-launchers.sh`

**Step 1: Add failing Windows launcher checks**

Extend the launcher test script to verify the Windows launcher content and dry-run behavior for:

- `MILLI_LAUNCHER_MODE`
- `MILLI_LAUNCHER_SOURCE`
- local version files
- native Windows artifact mapping
- `dist` fallback

**Step 2: Run launcher tests to verify Windows cases fail**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: Windows-specific assertions fail until `milli.bat` is updated.

**Step 3: Implement Windows launcher resolution**

Update `launcher/milli.bat` to mirror the Unix launcher behavior as closely as batch allows:

- local version file lookup
- source preference and fallback
- Windows native artifact selection
- `dist` execution via `java -jar`
- `.netrc` opt-in support where the downloader path supports it

**Step 4: Re-run launcher tests**

Run:

```bash
bash scripts/ci/test-launchers.sh
```

Expected: Windows launcher assertions pass.

**Step 5: Commit**

```bash
git add launcher/milli.bat scripts/ci/test-launchers.sh
git commit -m "feat: add windows milli launcher resolution logic"
```

### Task 5: Update Release Helpers, Workflows, and Documentation

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `.github/workflows/publish-central.yml`
- Modify: `scripts/ci/build-release-assembly.sh`
- Modify: `scripts/ci/compute-assembly-name.sh`
- Modify: `README.md`
- Modify: `docs/contributing/releasing.md`

**Step 1: Add failing release/docs expectations**

Update docs and helper expectations to use `dist` naming instead of `assembly` naming.

If necessary, add or extend a lightweight script assertion for release asset naming.

**Step 2: Run relevant verification commands to confirm the mismatch**

Run:

```bash
scripts/ci/test-publish-metadata.sh 1.2.3
bash scripts/ci/test-launchers.sh
```

Expected: any remaining references to `assembly` or `assemblyPublish` are surfaced.

**Step 3: Implement workflow and docs updates**

Change:

- release asset helper names from assembly to dist
- workflow task invocations and artifact upload paths
- publishing workflow module names from `assemblyPublish` to `distPublish`
- README and release docs to describe native-first launcher behavior, version files, source selection, and `.netrc` opt-in

Keep the current binary and GitHub release native archive family unchanged unless explicitly required by a separate issue.

**Step 4: Run full verification**

Run:

```bash
COURSIER_CACHE=/tmp/coursier ./mill --no-server test
scripts/ci/test-publish-metadata.sh 1.2.3
bash scripts/ci/test-launchers.sh
scripts/ci/build-release-notes.sh 0.1.0
bash scripts/ci/test-build-release-notes.sh
```

Expected: all commands pass.

**Step 5: Commit**

```bash
git add .github/workflows/release.yml .github/workflows/publish-central.yml scripts/ci/build-release-assembly.sh scripts/ci/compute-assembly-name.sh README.md docs/contributing/releasing.md
git commit -m "ci: align launcher release flow with dist artifacts"
```
