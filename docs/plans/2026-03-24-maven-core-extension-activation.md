# Maven Core Extension Activation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `mill-interceptor-maven-plugin` work as a true Maven core extension so `.mvn/extensions.xml` alone is enough for lifecycle interception and operational commands.

**Architecture:** Add real Maven core-extension bootstrap and lifecycle interception on top of the existing config loader, neutral execution request model, resolver, and Kyo-native runner. Keep the current Mojo layer as a thin boundary and helper surface rather than building a second planning stack.

**Tech Stack:** Scala 3, Mill declarative build, Maven core extension APIs, Maven plugin APIs, Kyo, zio-test

---

### Task 1: Add the extension bootstrap surface

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Modify: `mill-build/src/build/Modules.scala`
- Create: any required extension metadata resources under `modules/mill-interceptor-maven-plugin/resources/`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

**Step 1: Write the failing test**

Extend the Maven plugin module tests so they assert the artifact includes the
core-extension bootstrap metadata required for `.mvn/extensions.xml` loading.

**Step 2: Run the test to verify it fails**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginModuleSpec
```

Expected: FAIL because the artifact is still only shaped like a regular plugin.

**Step 3: Write the minimal implementation**

Add the extension bootstrap packaging and metadata needed for Maven to load the
artifact as a core extension while preserving the existing plugin packaging.

**Step 4: Run the test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginModuleSpec
```

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add modules/mill-interceptor-maven-plugin/package.mill.yaml mill-build/src/build/Modules.scala modules/mill-interceptor-maven-plugin/resources modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "feat: add maven core extension bootstrap"
```

### Task 2: Intercept the common lifecycle through the extension path

**Files:**
- Create or modify the extension bootstrap implementation under `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/...`
- Reuse and, if needed, minimally adjust:
  - `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/`
  - `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolver.scala`
  - `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Test: unit tests for extension-side request translation and failure mapping

**Step 1: Write the failing tests**

Add focused tests that prove the extension path:
- identifies the common lifecycle phases
- converts Maven state into the existing neutral `ExecutionRequest`
- routes failures into Maven-friendly diagnostics

**Step 2: Run the tests to verify they fail**

Run the new focused test target for the extension-side unit tests.

Expected: FAIL because the extension currently does not intercept lifecycle
execution.

**Step 3: Write the minimal implementation**

Implement the extension-side lifecycle interception that:
- observes the requested lifecycle phase
- builds the neutral request
- loads config
- resolves the `MillExecutionPlan`
- executes it through the existing runner

**Step 4: Run the tests to verify they pass**

Run the focused extension unit tests again.

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "feat: intercept maven lifecycle through extension"
```

### Task 3: Make the extension-only fixture pass

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle/**`
- Optionally modify: supporting fixture or test helper files

**Step 1: Use the already-added failing integration test**

The current minimal fixture already fails at extension-only activation.

**Step 2: Run the test to confirm the failure mode**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: FAIL until the extension path is fully wired.

**Step 3: Make the minimal fixture pass**

Adjust the extension implementation and fixture only as needed so that:
- `.mvn/extensions.xml` alone is enough
- `mvn validate`
- `mvn compile`
- `mvn mill-interceptor:inspect-plan`

all pass honestly through the extension path.

**Step 4: Run the test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: PASS for the minimal extension-only scenario.

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "test: prove extension-only maven activation"
```

### Task 4: Close out the blocker and resume fixture/docs work

**Files:**
- Verify only for this blocker

**Step 1: Run verification**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.test
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat
```

Expected: PASS

**Step 2: Update tracker state**

Run:

```bash
bd close MI-ajn --reason "Completed" --json
bd dolt push --json
```

Expected: issue closes and tracker state pushes successfully.

**Step 3: Push branch**

Run:

```bash
git -C .worktrees/mi-okw-8-fixtures-docs push
git -C .worktrees/mi-okw-8-fixtures-docs status -sb
```

Expected: branch is up to date with origin and the worktree is clean.

**Step 4: Resume `MI-okw.8`**

With extension-only activation working, continue the original fixtures-and-docs
task without having to fake the minimal setup story.
