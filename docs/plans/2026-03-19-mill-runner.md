# Mill Runner Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a Kyo-native `MillRunner` that executes `MillExecutionPlan` steps through live-streamed subprocesses and returns structured execution results.

**Architecture:** Keep the runner inside the Maven plugin module as a step executor over the existing neutral plan model. Use explicit command vectors, a typed result model, and a small injected process boundary so the runner can be tested without relying on heavyweight real subprocess fixtures.

**Tech Stack:** Scala 3, Kyo, Mill, zio-test

---

### Task 1: Add runner result and execution models

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerResult.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerResultSpec.scala`

**Step 1: Write the failing test**

Add tests that lock down:
- step results with rendered command vectors
- success and failure result shapes
- typed failure variants for fail, probe, and invocation failures

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the runner result model does not exist yet

**Step 3: Write minimal implementation**

Add the smallest result and failure model needed by the runner.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS for the new result-model tests

### Task 2: Add dry-run command rendering

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`

**Step 1: Write the failing dry-run tests**

Cover:
- probe step renders `mill resolve <target>`
- invoke step renders `mill <targets...>`
- configured executable and working directory are reflected in the rendered command

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the runner does not exist yet

**Step 3: Write minimal dry-run implementation**

Implement command rendering and dry-run execution without spawning real subprocesses.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS for the dry-run runner tests

### Task 3: Execute plan steps with failures

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`

**Step 1: Write the failing execution tests**

Cover:
- fail-step short-circuiting
- probe failure returns a typed failure
- invocation failure returns a typed failure
- successful steps accumulate ordered results

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL on the new execution assertions

**Step 3: Write minimal execution implementation**

Add a small injected subprocess boundary and implement the step executor using Kyo effects.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS

### Task 4: Verify formatting and commit

**Files:**
- Modify: `docs/plans/2026-03-19-mill-runner-design.md`
- Modify: `docs/plans/2026-03-19-mill-runner.md`

**Step 1: Run verification**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat`
Expected: PASS

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS

**Step 2: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec \
  modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec \
  docs/plans/2026-03-19-mill-runner-design.md \
  docs/plans/2026-03-19-mill-runner.md
git commit -m "feat: add mill execution runner"
```
