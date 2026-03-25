# Maven Plugin Fixtures And Docs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add end-to-end Maven fixtures and user-facing docs that prove and explain the real Maven lifecycle-forwarding plugin.

**Architecture:** Keep the existing Maven plugin integration test harness and extend it with checked-in fixtures copied into temp directories at runtime. Pair that with a README section and a dedicated user-facing Maven plugin guide so the documented setup matches the tested setup.

**Tech Stack:** Scala 3, Mill declarative build, Maven, Kyo, zio-test, Markdown docs

---

### Task 1: Add the minimal extension-only fixture

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle/.mvn/extensions.xml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle/pom.xml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle/build.mill.yaml`
- Create: any minimal supporting Mill source files needed by the fixture
- Test: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Write the failing integration assertions**

Extend the integration spec with a test that:
- installs the local Maven plugin artifact into a temp local repo
- copies `fixtures/minimal-lifecycle`
- runs `mvn validate`, `mvn compile`, `mvn test`, and `mvn mill-interceptor:inspect-plan`
- asserts success and forwarding-visible output

**Step 2: Run the test to verify it fails**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: FAIL because the minimal lifecycle fixture does not exist yet.

**Step 3: Add the minimal fixture**

Create the checked-in fixture with:
- `.mvn/extensions.xml`
- no YAML or PKL config
- a realistic Mill module layout
- baseline-friendly query targets in mind:
  - `clean`
  - `__.checkFormat`
  - `__.compile`
  - `__.test`

Use the fixture to test whether the conventional baseline is realistic without
falling back to toy top-level target names.

**Step 4: Run the test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: PASS for the minimal fixture path.

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "test: add minimal maven lifecycle fixture"
```

### Task 2: Add the multi-module override fixture and strict-failure path

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides/.mvn/extensions.xml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides/mill-interceptor.yaml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides/build.mill.yaml`
- Create: module-local fixture files under `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides/...`
- Test: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Write the failing integration assertions**

Add tests that:
- run Maven against the multi-module fixture
- prove module-local override behavior
- prove a strict-failure path with useful diagnostics
- optionally prove validate Scalafmt disablement if represented in the fixture

**Step 2: Run the test to verify it fails**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: FAIL because the override fixture and assertions do not exist yet.

**Step 3: Add the fixture and minimal harness changes**

Create the checked-in multi-module fixture and update the integration harness
only as much as needed to:
- copy the fixture
- run targeted Maven commands
- assert module override and failure behavior clearly

**Step 4: Run the test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: PASS for both the happy-path and strict-failure cases.

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "test: add maven override fixture coverage"
```

### Task 3: Document the Maven plugin in README and usage docs

**Files:**
- Modify: `README.md`
- Create: `docs/usage/maven-plugin.md`

**Step 1: Write the failing doc expectations**

Before editing, identify the missing coverage in the current docs:
- no real Maven plugin setup section in `README.md`
- no user-facing plugin guide covering minimal setup and overrides

**Step 2: Update the README**

Add a Maven plugin section covering:
- what the plugin does
- the minimal extension-only setup
- the common lifecycle baseline
- `inspect-plan`
- the validate Scalafmt hook and disable path

**Step 3: Add the user-facing Maven plugin guide**

Create `docs/usage/maven-plugin.md` with:
- `.mvn/extensions.xml` example
- no-config minimal setup example
- repo-level override example
- module-level override example
- config discovery locations

**Step 4: Review the docs for consistency with the fixtures**

Check that the documented examples match the checked-in fixtures and current
plugin behavior.

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-8-fixtures-docs add README.md docs/usage/maven-plugin.md
git -C .worktrees/mi-okw-8-fixtures-docs commit -m "docs: add maven plugin usage guide"
```

### Task 4: Run verification and close out the task

**Files:**
- Verify only

**Step 1: Run the plugin integration and format checks**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
./mill --no-server modules.mill-interceptor-maven-plugin.test
./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat
```

Expected: PASS

**Step 2: Review acceptance criteria**

Confirm that:
- the minimal extension-only path works without YAML or PKL
- the override fixture proves repo and module layering
- strict failure behavior is covered
- README and user-facing docs explain the actual current plugin usage

**Step 3: Update tracker state**

Run:

```bash
bd close MI-okw.8 --reason "Completed" --json
bd dolt push --json
```

Expected: issue closes and tracker state pushes successfully.

**Step 4: Push branch**

Run:

```bash
git -C .worktrees/mi-okw-8-fixtures-docs push
git -C .worktrees/mi-okw-8-fixtures-docs status -sb
```

Expected: branch is up to date with origin and the worktree is clean.
