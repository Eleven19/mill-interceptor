# Milli Release Publishing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add GitHub Release publication of an executable assembly jar, publish a new `milli` artifact family to Maven Central, and ship `milli` / `milli.bat` launchers that acquire the published assembly artifact.

**Architecture:** Extend the existing Mill release support so the build owns assembly, native, launcher, and publish-artifact metadata. Keep GitHub Release publication in the current workflow, add a separate `publish-central.yml` workflow for Maven Central, and preserve the current `mill-interceptor` native asset naming while introducing `milli` Maven coordinates and launcher UX.

**Tech Stack:** Mill 1.1.x, Scala 3, Mill `NativeImageModule`, Mill Central publishing support, GitHub Actions, shell scripts in `scripts/ci/`, Maven Central, GPG/signing secrets, tar/zip packaging

---

### Task 1: Add Build Metadata for the `milli` Artifact Family

**Files:**
- Modify: `build.mill.yaml`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Test: `./mill show releaseTargets`

**Step 1: Write the failing metadata check**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show publishArtifacts'
```

Expected: FAIL because the build does not yet expose Central publication metadata.

**Step 2: Add artifact identity metadata**

Extend `ReleaseSupport.scala` with explicit constants and helpers for:

```scala
private val mavenGroup = "io.github.eleven19.mill-interceptor"
private val libraryArtifact = "milli"
private val assemblyArtifact = "milli-assembly"
```

Also add a target-to-artifact-id mapping for native publications:

```scala
private def nativeArtifactNameFor(target: String): String =
  target match
    case "x86_64-unknown-linux-gnu" => "milli-native-linux-amd64"
    case "aarch64-unknown-linux-gnu" => "milli-native-linux-aarch64"
    case "x86_64-apple-darwin" => "milli-native-macos-amd64"
    case "aarch64-apple-darwin" => "milli-native-macos-aarch64"
    case "x86_64-pc-windows-msvc" => "milli-native-windows-amd64"
```

**Step 3: Add observable build tasks**

Expose simple Mill tasks that print the publication surface:

```scala
def publishGroup = Task { mavenGroup }
def publishLibraryArtifact = Task { libraryArtifact }
def publishAssemblyArtifact = Task { assemblyArtifact }
def publishNativeArtifacts = Task { releaseTargets().map(t => t -> nativeArtifactNameFor(t)) }
```

**Step 4: Verify the build surface exists**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show publishGroup publishLibraryArtifact publishAssemblyArtifact publishNativeArtifacts'
```

Expected: PASS and prints the approved group plus all `milli` artifact names.

**Step 5: Commit**

```bash
git add build.mill.yaml mill-build/src/build/ReleaseSupport.scala
git commit -m "build: add milli publication metadata"
```

### Task 2: Add Executable Assembly Jar Support

**Files:**
- Modify: `build.mill.yaml`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Modify: `scripts/ci/build-release-archive.sh`
- Create: `scripts/ci/build-release-assembly.sh`
- Test: `scripts/ci/build-release-assembly.sh`

**Step 1: Write the failing assembly check**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseAssemblyAssetName --version 1.2.3'
```

Expected: FAIL because no assembly release task exists yet.

**Step 2: Add assembly naming and output tasks**

Extend `ReleaseSupport.scala` with:

```scala
private def assemblyAssetNameFor(version: String): String =
  s"mill-interceptor-assembly-v$version.jar"

def releaseAssemblyAssetName(version: String) = Task.Command {
  assemblyAssetNameFor(version)
}

def releaseAssembly(version: String) = Task.Command {
  val destination = Task.dest / assemblyAssetNameFor(version)
  os.copy.over(assembly().path, destination, createFolders = true)
  PathRef(destination)
}
```

Adjust the exact source task if the build uses a different assembly entry point than `assembly()`.

**Step 3: Add a shell wrapper matching the native archive helpers**

Create `scripts/ci/build-release-assembly.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"
asset_name="${2:?asset name is required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill show releaseAssembly \
  --version "$version"

test -f "out/releaseAssembly.dest/$asset_name"
```

**Step 4: Verify local assembly generation**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseAssemblyAssetName --version 1.2.3'
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseAssembly --version 1.2.3'
```

Expected: PASS and the jar appears in `out/releaseAssembly.dest/`.

**Step 5: Commit**

```bash
git add build.mill.yaml mill-build/src/build/ReleaseSupport.scala scripts/ci/build-release-assembly.sh scripts/ci/build-release-archive.sh
git commit -m "build: add release assembly jar support"
```

### Task 3: Add Launcher Script Sources and Generation

**Files:**
- Create: `launcher/milli`
- Create: `launcher/milli.bat`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Create: `scripts/ci/test-launchers.sh`
- Test: `scripts/ci/test-launchers.sh`

**Step 1: Write the failing launcher verification**

Create `scripts/ci/test-launchers.sh` with initial checks:

```bash
#!/usr/bin/env bash
set -euo pipefail

test -f launcher/milli
test -f launcher/milli.bat
```

Run it.

Expected: FAIL because the launchers do not exist yet.

**Step 2: Add launcher scripts**

Create `launcher/milli` and `launcher/milli.bat` that:

- resolve a version argument or embedded default version
- download `io.github.eleven19.mill-interceptor:milli-assembly:<version>`
- reuse a local cache directory
- execute the jar with `java -jar`

Keep the first pass simple and explicit. Avoid adding update channels or self-modifying behavior.

**Step 3: Add build tasks that stage launcher assets**

Expose tasks in `ReleaseSupport.scala` such as:

```scala
def releaseLauncherName(os: String) = Task.Command { ... }
def releaseLauncher(version: String, os: String) = Task.Command { ... }
```

The task should copy the checked-in launcher source into `Task.dest`, optionally stamping the release version if the script embeds a default version string.

**Step 4: Verify launcher assets**

Run:

```bash
scripts/ci/test-launchers.sh
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseLauncher --version 1.2.3 --os unix'
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseLauncher --version 1.2.3 --os windows'
```

Expected: PASS and staged launcher files appear under `out/releaseLauncher.dest/`.

**Step 5: Commit**

```bash
git add launcher/milli launcher/milli.bat mill-build/src/build/ReleaseSupport.scala scripts/ci/test-launchers.sh
git commit -m "feat: add milli launcher scripts"
```

### Task 4: Introduce Maven Central Publishing Support

**Files:**
- Modify: `build.mill.yaml`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Test: `./mill show publishArtifacts`

**Step 1: Write the failing publish-surface check**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show publishArtifacts'
```

Expected: FAIL or incomplete output because the build does not yet declare publishable artifacts.

**Step 2: Add Mill publish support**

Extend the root build to use the appropriate Mill publishing module and metadata:

```scala
extends ..., mill.javalib.publish.SonatypeCentralPublishModule

def pomSettings = PomSettings(
  description = "A tool for intercepting other build tools using mill.",
  organization = "io.github.eleven19.mill-interceptor",
  url = "https://github.com/Eleven19/mill-interceptor",
  licenses = Seq(License.`Apache-2.0`),
  versionControl = VersionControl.github("Eleven19", "mill-interceptor"),
  developers = Seq(Developer("Damian Reeves", "...", "..."))
)
```

Then define `publishArtifacts` so it includes:

- the regular library artifact as `milli`
- the assembly jar as `milli-assembly`
- one native artifact per target

If the root module artifact name is still derived from the repo/module name, override it explicitly to `milli`.

**Step 3: Expose a deterministic publish-artifact listing task**

If `publishArtifacts` output is hard to inspect directly, add:

```scala
def publishArtifactSummary = Task {
  publishArtifacts().map(a => s"${a.artifact.id}:${a.file.path.last}")
}
```

**Step 4: Verify publication metadata**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show publishArtifactSummary'
```

Expected: PASS and prints all approved artifact IDs.

**Step 5: Commit**

```bash
git add build.mill.yaml mill-build/src/build/ReleaseSupport.scala
git commit -m "build: add maven central publication support"
```

### Task 5: Extend the GitHub Release Workflow for Assembly and Launchers

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `scripts/ci/build-release-archive.sh`
- Modify: `scripts/ci/upload-release-assets.sh`
- Create: `scripts/ci/compute-assembly-name.sh`
- Create: `scripts/ci/compute-launcher-name.sh`
- Test: `scripts/ci/build-release-notes.sh`

**Step 1: Write the failing release asset expectation**

Inspect the current workflow and confirm it only uploads native archives:

```bash
rg -n "release-artifacts|upload-release-assets|build-release-assembly|launcher" .github/workflows/release.yml scripts/ci
```

Expected: no assembly or launcher handling yet.

**Step 2: Add helper scripts for new asset names**

Create scripts that mirror the current archive-name helper pattern:

- `scripts/ci/compute-assembly-name.sh`
- `scripts/ci/compute-launcher-name.sh`

Each should call the corresponding Mill task and emit a GitHub Actions output value.

**Step 3: Update the workflow build/publish steps**

Modify `.github/workflows/release.yml` so it:

- computes the assembly asset name
- builds the assembly jar once
- stages launcher assets once
- uploads the native archives, assembly jar, and launchers as artifacts
- includes them all in the publish job before checksum generation or upload

Prefer a dedicated non-matrix build step for assembly and launchers rather than rebuilding them on every native target leg.

**Step 4: Verify workflow shape locally**

Run:

```bash
rg -n "assembly|launcher|SHA256SUMS|upload-artifact|download-artifact" .github/workflows/release.yml scripts/ci
```

Expected: the workflow now clearly includes assembly and launcher assets.

**Step 5: Commit**

```bash
git add .github/workflows/release.yml scripts/ci/build-release-archive.sh scripts/ci/upload-release-assets.sh scripts/ci/compute-assembly-name.sh scripts/ci/compute-launcher-name.sh
git commit -m "ci: add assembly and launcher release assets"
```

### Task 6: Add a Separate Maven Central Workflow

**Files:**
- Create: `.github/workflows/publish-central.yml`
- Create: `scripts/ci/publish-central.sh`
- Test: `.github/workflows/publish-central.yml`

**Step 1: Write the failing workflow presence check**

Run:

```bash
test -f .github/workflows/publish-central.yml
```

Expected: FAIL because the workflow does not exist yet.

**Step 2: Add the Central workflow**

Create `.github/workflows/publish-central.yml` that:

- triggers on tags matching `v*`
- normalizes the version
- sets up Java
- exports organization publication secrets into environment variables
- invokes `scripts/ci/publish-central.sh`

Keep the secret names isolated in workflow `env:` so they can be changed without touching build logic.

**Step 3: Add the publish wrapper**

Create `scripts/ci/publish-central.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

version="${1:?version is required}"

: "${CENTRAL_PORTAL_USERNAME:?required}"
: "${CENTRAL_PORTAL_PASSWORD:?required}"
: "${CENTRAL_SIGNING_KEY:?required}"
: "${CENTRAL_SIGNING_PASSWORD:?required}"

COURSIER_CACHE="${RUNNER_TEMP:-/tmp}/coursier" ./mill publishAll \
  --version "$version"
```

Adjust the exact Mill publish entry point to match the chosen module.

**Step 4: Verify workflow wiring**

Run:

```bash
sed -n '1,240p' .github/workflows/publish-central.yml
sed -n '1,220p' scripts/ci/publish-central.sh
```

Expected: workflow clearly shows tag trigger, version normalization, secret wiring, and publish invocation.

**Step 5: Commit**

```bash
git add .github/workflows/publish-central.yml scripts/ci/publish-central.sh
git commit -m "ci: add maven central publish workflow"
```

### Task 7: Add Tests for Release and Publishing Metadata

**Files:**
- Create: `test/src/build/ReleaseSupportSpec.scala`
- Modify: `scripts/ci/test-build-release-notes.sh`
- Test: `test/src/build/ReleaseSupportSpec.scala`

**Step 1: Write the failing spec scaffold**

Create `test/src/build/ReleaseSupportSpec.scala` with expectations like:

```scala
test("native artifact id for linux amd64") {
  assertEquals(nativeArtifactNameFor("x86_64-unknown-linux-gnu"), "milli-native-linux-amd64")
}
```

Expected: FAIL because the metadata helpers are not yet testable from a spec.

**Step 2: Refactor helper logic for testability**

Move pure naming logic in `ReleaseSupport.scala` into visible methods or a small helper object under `mill-build/src/build/` so the test suite can import it without invoking full packaging tasks.

Cover:

- Central artifact IDs
- release assembly asset name
- launcher asset names
- native archive asset names

**Step 3: Run the targeted tests**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server test.testOnly build.ReleaseSupportSpec'
```

Expected: PASS with deterministic naming assertions.

**Step 4: Commit**

```bash
git add test/src/build/ReleaseSupportSpec.scala mill-build/src/build/ReleaseSupport.scala scripts/ci/test-build-release-notes.sh
git commit -m "test: cover release publishing metadata"
```

### Task 8: Update Documentation for Release and Install Flows

**Files:**
- Modify: `README.md`
- Modify: `docs/contributing/releasing.md`
- Modify: `CHANGELOG.md`
- Test: `scripts/ci/build-release-notes.sh`

**Step 1: Write the failing documentation check**

Inspect current docs for missing assembly, Central, and launcher references:

```bash
rg -n "maven central|assembly|milli|launcher|native releases" README.md docs/contributing/releasing.md
```

Expected: docs only mention native GitHub Releases.

**Step 2: Update user-facing documentation**

Add:

- `milli` launcher usage to `README.md`
- Central publication flow and required secrets to `docs/contributing/releasing.md`
- release artifact summary for native archives, assembly jar, and launchers

Update `CHANGELOG.md` only if this work is landing as part of a release-ready branch with a versioned entry; otherwise leave changelog promotion to the release-cut task.

**Step 3: Verify release note generation still works**

Run:

```bash
scripts/ci/build-release-notes.sh 0.1.0
bash scripts/ci/test-build-release-notes.sh
```

Expected: PASS and no regression in release note assembly.

**Step 4: Commit**

```bash
git add README.md docs/contributing/releasing.md CHANGELOG.md
git commit -m "docs: document milli publishing and launchers"
```

### Task 9: Run Verification Before Completion

**Files:**
- Verify only

**Step 1: Run the test suite**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server test'
```

Expected: PASS.

**Step 2: Run targeted release checks**

Run:

```bash
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show releaseAssemblyAssetName --version 1.2.3'
/bin/bash -lc 'COURSIER_CACHE=/tmp/coursier ./mill --no-server show publishArtifactSummary'
scripts/ci/test-launchers.sh
```

Expected: PASS.

**Step 3: Review changed files**

Run:

```bash
git status --short
git diff --stat
```

Expected: only intended files changed.

**Step 4: Final commit**

```bash
git add .
git commit -m "feat: add milli release and central publishing"
```
