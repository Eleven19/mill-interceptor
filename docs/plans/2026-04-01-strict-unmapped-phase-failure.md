# Strict Unmapped Phase Failure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Maven plugin integration coverage proving strict mode rejects explicitly unmapped Maven phases with clear end-to-end diagnostics.

**Architecture:** Keep the current missing-target strict failure coverage intact and add a separate fixture for explicit `unmapped` lifecycle mappings. Drive a small matrix of Maven phase invocations from one integration test so the same extension-only fixture proves the strict rejection path for multiple phases.

**Tech Stack:** Scala 3, ZIO Test, Maven plugin integration fixtures, `.mvn/extensions.xml`, YAML interceptor config

---

Issue: `MI-fuw`

### Task 1: Add the failing strict-unmapped fixture and test

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases/.mvn/extensions.xml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases/build.sc`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases/mill-interceptor.yaml`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases/pom.xml`
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Write the failing test**

Add a new integration test that:

- copies the new `strict-unmapped-phases` fixture
- installs the required local artifacts once
- runs Maven for `compile`, `test`, and `package`
- asserts each command fails with strict unmapped diagnostics

Use assertions that look for:

- non-zero exit
- `BUILD FAILURE`
- `strict`
- the phase name
- absence of `missing.`

**Step 2: Run test to verify it fails**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: the new test fails because the fixture or assertions do not yet match
the real strict unmapped diagnostic path.

**Step 3: Write minimal fixture implementation**

Add the new fixture with:

- `.mvn/extensions.xml` using the existing snapshot coordinates
- a minimal `build.sc`
- a minimal `pom.xml`
- `mill-interceptor.yaml` with `strict: true` and explicit `unmapped`
  mappings for `compile`, `test`, and `package`

**Step 4: Run test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: the new strict unmapped matrix test passes and existing integration
tests remain green.

### Task 2: Tighten assertions to distinguish unmapped-phase failures from missing-target failures

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Write the failing assertion refinement**

Tighten the new test so it does not merely check `BUILD FAILURE`. Require phase
and strict diagnostic text, and reject `missing.`-style output.

**Step 2: Run test to verify it fails if the diagnostic is too weak**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: fail until the assertions match the actual strict unmapped failure
shape.

**Step 3: Adjust test minimally**

Refine the assertion helper or per-phase assertions to match the actual end-to-end
diagnostic wording without weakening the distinction between explicit unmapped
failures and missing-target failures.

**Step 4: Run test to verify it passes**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: pass with stable, specific assertions.

### Task 3: Verify formatting and full Maven plugin integration coverage

**Files:**
- Verify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`
- Verify: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases/*`

**Step 1: Run formatting verification**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat
```

Expected: PASS

**Step 2: Run integration verification**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: PASS

**Step 3: Commit and push**

Run:

```bash
git add docs/plans/2026-04-01-strict-unmapped-phase-failure-design.md \
  docs/plans/2026-04-01-strict-unmapped-phase-failure.md \
  modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala \
  modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-unmapped-phases
git commit -m "test: cover strict unmapped Maven phase failures"
git pull --rebase
bd dolt push --json
git push
git status -sb
```

Expected: branch clean or only intentional untracked files, local branch synced
with `origin`.
