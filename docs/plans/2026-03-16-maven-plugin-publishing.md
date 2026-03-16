# Maven Plugin Publishing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Publish `modules/mill-interceptor-maven-plugin` as a real Maven plugin artifact under `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin`, with generated plugin metadata and integration tests that prove a fixture Maven build can resolve and execute a placeholder goal.

**Architecture:** Keep the current multi-module split intact. Extend the Maven plugin module from a plain Scala placeholder into a real plugin-publish surface, add the smallest annotated placeholder Mojo set needed for descriptor generation, and verify it through both module tests and Maven fixture integration tests. Preserve the current interceptor product’s publish surface and CI behavior while adding the new plugin artifact to publish orchestration.

**Tech Stack:** Mill declarative YAML plus shared Scala build traits in `mill-build/`, Scala 3, Maven plugin annotations/metadata, Kyo, ZIO Test, Maven CLI, GitHub Actions, beads

---

### Task 1: Add Regression Checks For The New Published Artifact

**Files:**
- Modify: `scripts/ci/test-publish-metadata.sh`
- Modify: `scripts/ci/test-release-workflows.sh`
- Modify: `docs/contributing/releasing.md`

**Step 1: Write the failing publish metadata expectation**

Extend `scripts/ci/test-publish-metadata.sh` so it expects:
- `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin:<version>`
- a publish task for `modules.mill-interceptor-maven-plugin.publishSonatypeCentral`

Do not implement the build change yet.

**Step 2: Run the metadata script to verify it fails**

Run: `scripts/ci/test-publish-metadata.sh 1.2.3`
Expected: FAIL because the plugin artifact is not yet part of the publish
surface.

**Step 3: Extend release workflow assertions**

Update `scripts/ci/test-release-workflows.sh` and the active release doc only as
needed so they describe the new Central publish surface without claiming GitHub
release assets changed.

**Step 4: Commit**

```bash
git add scripts/ci/test-publish-metadata.sh scripts/ci/test-release-workflows.sh docs/contributing/releasing.md
git commit -m "Add Maven plugin publish regression checks"
```

### Task 2: Upgrade The Plugin Module Build To A Real Maven Plugin Artifact

**Files:**
- Modify: `mill-build/src/build/Modules.scala`
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Modify: `scripts/ci/test-publish-central.sh`
- Modify: `.github/workflows/publish-central.yml`

**Step 1: Add the failing build wiring**

Extend the shared build support so `modules/mill-interceptor-maven-plugin` has:
- explicit artifactId `mill-interceptor-maven-plugin`
- publish support under the existing group/version flow
- any plugin-specific dependency/packaging wiring required for descriptor
  generation

At this step, it is acceptable if publish-related commands fail because the
plugin implementation/metadata is still incomplete.

**Step 2: Run publish task resolution and verify the expected red state**

Run: `./mill --no-server resolve __.publishSonatypeCentral`
Expected: either the new plugin publish task is missing or subsequent packaging
verification still fails because plugin metadata is incomplete.

**Step 3: Update Central publish orchestration**

Wire the plugin module into:
- `.github/workflows/publish-central.yml`
- `scripts/ci/test-publish-central.sh`

using the exact module task path.

**Step 4: Commit**

```bash
git add mill-build/src/build/Modules.scala modules/mill-interceptor-maven-plugin/package.mill.yaml scripts/ci/test-publish-central.sh .github/workflows/publish-central.yml
git commit -m "Wire Maven plugin module into publish flow"
```

### Task 3: Add Placeholder Mojos With TDD

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/<PlaceholderMojo>.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`
- Create: additional unit tests under `modules/mill-interceptor-maven-plugin/test/src/...` as needed

**Step 1: Write the failing unit test first**

Extend the plugin test suite to assert one concrete placeholder behavior, such
as:
- a placeholder Mojo class exists
- it advertises the expected goal name through its annotation/metadata
- the plugin module exposes the expected artifact identity

Keep the test focused on one real plugin-facing behavior.

**Step 2: Run the plugin tests to verify they fail**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the placeholder Mojo or metadata is missing.

**Step 3: Add the minimal placeholder Mojo implementation**

Create the annotated placeholder Mojo with the smallest implementation that
allows Maven plugin metadata generation and satisfies the test.

**Step 4: Re-run the plugin tests to verify green**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src modules/mill-interceptor-maven-plugin/test/src
git commit -m "Add placeholder Maven plugin goals"
```

### Task 4: Prove Descriptor Generation And Publish Metadata

**Files:**
- Modify: only files required to fix missed plugin packaging assumptions

**Step 1: Re-run publish task resolution**

Run: `./mill --no-server resolve __.publishSonatypeCentral`
Expected: includes:
- `modules.mill-interceptor.publishSonatypeCentral`
- `modules.mill-interceptor-maven-plugin.publishSonatypeCentral`

**Step 2: Re-run publish metadata verification**

Run: `scripts/ci/test-publish-metadata.sh 1.2.3`
Expected: PASS and include the new plugin artifact.

**Step 3: Inspect plugin publish payload if needed**

Run the smallest command that proves the plugin jar and metadata are present in
the publish output, then adjust packaging only if necessary.

**Step 4: Commit**

```bash
git add mill-build modules/mill-interceptor-maven-plugin scripts/ci
git commit -m "Finalize Maven plugin publish metadata"
```

### Task 5: Add Maven Fixture Integration Tests

**Files:**
- Create: fixture files under `modules/mill-interceptor-maven-plugin/itest/` or another plugin-local integration-test location
- Create: any helper Scala or shell test glue needed for Maven invocation
- Modify: `mill-build/src/build/Modules.scala` and/or `modules/mill-interceptor-maven-plugin/package.mill.yaml` to wire plugin integration tests

**Step 1: Write the failing integration test first**

Create a Maven fixture project with a `pom.xml` that:
- references `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin`
- points at the locally published or locally installed plugin coordinates
- invokes a placeholder goal

Write the integration test to assert that Maven resolves the plugin and the goal
exits successfully.

**Step 2: Run the integration test to verify red**

Run the narrowest plugin integration test command.
Expected: FAIL because the local publish/install and plugin invocation path is
not fully wired yet.

**Step 3: Add the minimal local publish/install plumbing**

Implement only what is needed so the integration test can:
- make the plugin artifact available to Maven locally
- execute the placeholder goal successfully

**Step 4: Re-run the integration test to verify green**

Run the same targeted integration test command.
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin
git commit -m "Add Maven plugin integration tests"
```

### Task 6: Full Verification And CI Alignment

**Files:**
- Modify: only if full verification reveals missed task paths or metadata

**Step 1: Run formatting and module tests**

Run:
- `./mill --no-server __.checkFormat`
- `./mill --no-server __.test`
- `./mill --no-server modules.mill-interceptor.itest.testLocal`
- the plugin integration test command from Task 5

Expected: all PASS.

**Step 2: Run release/publish regression checks**

Run:
- `bash scripts/ci/test-publish-central.sh`
- `scripts/ci/test-publish-metadata.sh 1.2.3`
- `bash scripts/ci/test-release-workflows.sh`

Expected: all PASS and include the new plugin artifact.

**Step 3: Update PR branch and recheck GitHub CI**

Push the branch, inspect PR checks with `gh`, and fix any CI-specific fallout
before moving on.

**Step 4: Commit any final verification-driven fixes**

```bash
git add <fixed-files>
git commit -m "Fix Maven plugin publishing verification gaps"
```

### Task 7: Final Sync And Handoff

**Files:**
- Modify: none unless needed for final doc cleanup

**Step 1: Run final verification commands fresh**

Repeat the full verification set from Task 6 before claiming completion.

**Step 2: Update tracking**

If `bd` is healthy, record follow-up issues for:
- real lifecycle interception
- YAML/PKL mapping work
- expanded plugin goal surface

If `bd` is still broken, record the exact blocker in the handoff.

**Step 3: Sync and push**

Run:

```bash
git pull --rebase
bd dolt push
git push
git status
```

Expected:
- git push succeeds
- `git status` is clean
- if `bd dolt push` still fails, the failure is reported explicitly with cause
