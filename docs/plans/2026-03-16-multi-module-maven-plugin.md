# Multi-Module Maven Plugin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure the repository into a real multi-module Mill build with a root aggregate, move the current product into `modules/mill-interceptor`, and add a structural Maven plugin module at `modules/mill-interceptor-maven-plugin`.

**Architecture:** Keep `build.mill.yaml` as the declarative entrypoint, keep shared programmatic build code in `mill-build/`, and make product ownership explicit under `modules/`. Treat this as a structure-only change: preserve current interceptor behavior, preserve current publish ownership for `milli`, and add only a minimal compile/test skeleton for the future Maven plugin module.

**Tech Stack:** Mill declarative YAML, shared Mill Scala build traits in `mill-build/`, Scala 3, Kyo, scribe, ZIO Test, JUnit/Cucumber, beads

---

### Task 1: Document The Current Build Shape With Failing Structural Checks

**Files:**
- Create: `scripts/ci/test-multi-module-layout.sh`
- Modify: `README.md`

**Step 1: Write the failing layout regression script**

Create `scripts/ci/test-multi-module-layout.sh` with checks for:
- `modules/mill-interceptor/src`
- `modules/mill-interceptor/test`
- `modules/mill-interceptor/itest`
- `modules/mill-interceptor-maven-plugin/src`
- `modules/mill-interceptor-maven-plugin/test`
- `build.mill.yaml` declaring both module names

The script should fail if the old root-level `src`, `test`, or `itest` product
layout is still the active build shape.

**Step 2: Run the layout regression to verify it fails**

Run: `bash scripts/ci/test-multi-module-layout.sh`
Expected: FAIL because the repository is still single-module.

**Step 3: Add a short README note if task paths change**

Update `README.md` only if needed to mention that the build is now organized
under `modules/` while keeping the user-facing CLI behavior unchanged.

**Step 4: Commit**

```bash
git add scripts/ci/test-multi-module-layout.sh README.md
git commit -m "Add multi-module layout regression check"
```

### Task 2: Extract Shared Build Module Types Into `mill-build/`

**Files:**
- Create: `mill-build/src/build/Modules.scala`
- Modify: `mill-build/src/build/PublishSupport.scala`
- Modify: `mill-build/src/build/ReleaseSupport.scala`
- Modify: `build.mill.yaml`

**Step 1: Write the failing build wiring expectation**

Update `build.mill.yaml` and the new `mill-build/src/build/Modules.scala`
definition so the build has named module classes for:
- the aggregate/root module
- the interceptor product module
- the Maven plugin module
- shared test-module wiring where useful

At this step, it is acceptable for the build to fail because the moved source
trees do not exist yet.

**Step 2: Run Mill to verify the new module paths fail for the expected reason**

Run: `./mill resolve __.compile`
Expected: FAIL because the new module directories or source roots are not in
place yet, not because of syntax errors in the build definitions.

**Step 3: Move any root-specific release/publish assumptions behind the product module**

Refactor shared traits only as much as needed so `modules/mill-interceptor` can
own the existing release/publish tasks without the repository root being the
product module.

**Step 4: Commit**

```bash
git add build.mill.yaml mill-build/src/build/Modules.scala mill-build/src/build/PublishSupport.scala mill-build/src/build/ReleaseSupport.scala
git commit -m "Add shared multi-module build definitions"
```

### Task 3: Move The Existing Interceptor Product Into `modules/mill-interceptor`

**Files:**
- Create: `modules/mill-interceptor/`
- Create: `modules/mill-interceptor/package.mill.yaml`
- Create: `modules/mill-interceptor/test/package.mill.yaml`
- Create: `modules/mill-interceptor/itest/package.mill.yaml`
- Move: `src/**` to `modules/mill-interceptor/src/**`
- Move: `test/**` to `modules/mill-interceptor/test/**`
- Move: `itest/**` to `modules/mill-interceptor/itest/**`
- Delete: root `src/`
- Delete: root `test/`
- Delete: root `itest/`

**Step 1: Move the production sources**

Move the full production source tree from `src/` to
`modules/mill-interceptor/src/`.

**Step 2: Move the unit-test sources and module-local test YAML**

Move:
- `test/src/**` to `modules/mill-interceptor/test/src/**`
- `test/package.mill.yaml` to `modules/mill-interceptor/test/package.mill.yaml`

**Step 3: Move the integration-test sources and module-local test YAML**

Move:
- `itest/src/**` to `modules/mill-interceptor/itest/src/**`
- `itest/resources/**` to `modules/mill-interceptor/itest/resources/**`
- `itest/package.mill.yaml` to `modules/mill-interceptor/itest/package.mill.yaml`

**Step 4: Run the layout regression**

Run: `bash scripts/ci/test-multi-module-layout.sh`
Expected: PASS for directory layout checks, even if some build wiring still
needs final adjustment.

**Step 5: Commit**

```bash
git add modules/mill-interceptor
git rm -r src test itest
git commit -m "Move interceptor sources into modules directory"
```

### Task 4: Add A Minimal Maven Plugin Module Skeleton With TDD

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/package.mill.yaml`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

**Step 1: Write the failing placeholder test**

Create `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`
using the existing Kyo/ZIO test style. Assert one minimal structural behavior,
for example that:

- `MavenPluginModule.moduleName == "mill-interceptor-maven-plugin"`

Keep the test focused on the skeleton being present and loadable.

**Step 2: Run the plugin test to verify it fails**

Run: `./mill modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the placeholder production source does not exist yet.

**Step 3: Write the minimal production source**

Create `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala`
with the smallest implementation needed to satisfy the test, such as:

- an `object MavenPluginModule`
- a `val moduleName = "mill-interceptor-maven-plugin"`

No Maven lifecycle logic belongs in this step.

**Step 4: Run the plugin test to verify it passes**

Run: `./mill modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin
git commit -m "Add Maven plugin module skeleton"
```

### Task 5: Restore Full Build And Test Verification For The Moved Interceptor Module

**Files:**
- Modify: only files required to fix missed build path assumptions

**Step 1: Run the moved interceptor unit tests**

Run: `./mill modules.mill-interceptor.test`
Expected: PASS.

**Step 2: Run the moved interceptor integration tests**

Run: `./mill modules.mill-interceptor.itest.test`
Expected: PASS.

**Step 3: Run compile/package tasks for both modules**

Run: `./mill resolve __.compile`
Expected: PASS and list compile targets for:
- `modules.mill-interceptor.compile`
- `modules.mill-interceptor-maven-plugin.compile`

Then run:
- `./mill modules.mill-interceptor.compile`
- `./mill modules.mill-interceptor-maven-plugin.compile`

Expected: PASS.

**Step 4: Verify publish/release ownership remains with the moved interceptor module**

Run the repo-specific publish/release verification commands currently used for
the main product and confirm they now target `modules.mill-interceptor` rather
than the root module.

**Step 5: Apply the smallest path or task-alias fixes needed**

Limit changes to:
- build YAML wiring
- shared module definitions in `mill-build/`
- documentation or helper scripts that still assume root-level source trees

**Step 6: Commit**

```bash
git add build.mill.yaml mill-build README.md scripts/ci modules/mill-interceptor
git commit -m "Fix build wiring after module split"
```

### Task 6: Final Verification, Tracking, And Handoff

**Files:**
- Modify: only if verification reveals missed docs or path assumptions

**Step 1: Run the complete verification set**

Run:
- `bash scripts/ci/test-multi-module-layout.sh`
- `./mill modules.mill-interceptor.test`
- `./mill modules.mill-interceptor.itest.test`
- `./mill modules.mill-interceptor-maven-plugin.test`
- `./mill resolve __.compile`

Expected: all PASS.

**Step 2: Record follow-up work in beads**

If `bd` is healthy, create or update issues for:
- Maven lifecycle interception implementation
- YAML mapping support
- PKL mapping support
- plugin publishing decisions

If `bd` is still failing, record the exact blocker and do not silently skip it.

**Step 3: Run repository sync and push**

Run:

```bash
git pull --rebase
bd dolt push
git push
git status
```

Expected:
- pushes succeed
- `git status` shows a clean branch and up-to-date remote state

**Step 4: Hand off with explicit residual risks**

Call out any remaining gaps such as:
- root task alias compatibility choices
- whether the plugin module should become publishable next
- any `bd` tracking failure that still needs repair
