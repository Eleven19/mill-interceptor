# Shared Execution Plan Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extract the neutral execution-plan model out of the Maven plugin into a shared package without moving Maven-specific resolver or runner logic.

**Architecture:** Move only the domain types into `modules/mill-interceptor`, update Maven plugin imports to consume them, and keep all planning/execution policy in the Maven plugin. Validate the extraction by running Maven plugin and CLI test suites that compile against the new ownership boundary.

**Tech Stack:** Scala 3, Mill, Kyo, os-lib, zio-test/KyoSpec

---

### Task 1: Add the shared model package

**Files:**
- Create: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala`
- Modify: `modules/mill-interceptor/package.mill.yaml`
- Test: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/model/SharedExecutionPlanSpec.scala`

**Step 1: Write the failing test**

Write a new spec that constructs `ExecutionRequest`, `MillExecutionPlan`, and `PlanStep` from the new shared package and asserts their shape.

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.model.SharedExecutionPlanSpec`
Expected: FAIL because the shared model file does not exist yet.

**Step 3: Write minimal implementation**

Add the new shared model file by moving the neutral type definitions into the CLI module package.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.model.SharedExecutionPlanSpec`
Expected: PASS

**Step 5: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/model/SharedExecutionPlanSpec.scala modules/mill-interceptor/package.mill.yaml
git commit -m "refactor: add shared execution plan model"
```

### Task 2: Repoint Maven plugin imports

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolver.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/...` files that import the old package
- Delete or shrink: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MillExecutionPlan.scala`

**Step 1: Write the failing compile/test signal**

Switch one Maven plugin test import to the new shared package first.

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL until all production imports are updated.

**Step 3: Write minimal implementation**

Update Maven plugin production and test imports to use the shared model package. Remove the old local model definitions once nothing depends on them.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin
git commit -m "refactor: consume shared execution plan model"
```

### Task 3: Verify CLI ownership and docs

**Files:**
- Modify: `docs/plans/2026-03-25-shared-execution-plan-design.md`
- Modify: `docs/plans/2026-03-25-shared-execution-plan.md`
- Optionally modify: `modules/mill-interceptor/src/...` scaladoc if needed for clarity

**Step 1: Run focused verification**

Run:
- `./mill --no-server modules.mill-interceptor.test`
- `./mill --no-server modules.mill-interceptor-maven-plugin.test`
- `./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat`

Expected: PASS

**Step 2: Document the conclusion**

If the extraction is clean, keep the docs as implemented. If hidden coupling blocks it, update the design doc to record that the evaluation found the shared package should remain local for now.

**Step 3: Commit**

```bash
git add docs/plans modules/mill-interceptor modules/mill-interceptor-maven-plugin
git commit -m "docs: record shared execution plan evaluation"
```
