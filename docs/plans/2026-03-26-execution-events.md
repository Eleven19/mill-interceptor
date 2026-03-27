# Execution Events Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a shared execution-event model and integrate it into plan
resolution and runner execution without introducing a mediator or changing the
existing result model.

**Architecture:** Keep the shared request/plan model in
`modules.mill-interceptor` as the neutral boundary, add a small event ADT and
sink there, then thread a default no-op sink through the Maven resolver and
runner. Use recording sinks in tests to assert ordered emissions.

**Tech Stack:** Scala 3, Kyo, Mill, ScalaTest-style Kyo specs, shared model in
`modules/mill-interceptor`, Maven integration in
`modules/mill-interceptor-maven-plugin`

---

### Task 1: Add the shared event model

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala`
- Test: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/model/SharedExecutionPlanSpec.scala`

**Step 1: Add the new event ADT and sink**

Add:

- `ExecutionEvent`
- `ExecutionEventSink`
- `PlanResolved`
- `StepStarted`
- `StepFinished`
- `StepFailed`

Keep the shape minimal and colocated with the shared execution-plan model.

**Step 2: Extend the shared model spec**

Add assertions that:

- the event variants construct cleanly
- a recording sink preserves event order

**Step 3: Run the shared model tests**

Run: `./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.model.SharedExecutionPlanSpec`

Expected: PASS

**Step 4: Commit**

```bash
git -C .worktrees/mi-815-execution-events add modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/model/SharedExecutionPlanSpec.scala
git -C .worktrees/mi-815-execution-events commit -m "feat: add shared execution events"
```

### Task 2: Emit resolution events

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolver.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolverSpec.scala`

**Step 1: Thread a sink into resolver entrypoints**

Update `resolve` to accept a sink with a default no-op implementation.

**Step 2: Emit `PlanResolved`**

Publish one event after the plan is built and before it is returned.

**Step 3: Add resolver assertions**

Use a recording sink in the resolver spec and assert the resolved plan and
emitted event match.

**Step 4: Run the resolver spec**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.resolve.ExecutionPlanResolverSpec`

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-815-execution-events add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolver.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolverSpec.scala
git -C .worktrees/mi-815-execution-events commit -m "feat: emit plan resolved events"
```

### Task 3: Emit runner step events

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`

**Step 1: Thread the sink into `MillRunner.execute`**

Add a default no-op sink parameter.

**Step 2: Emit step events at the execution boundary**

Emit:

- `StepStarted` before probe/invoke steps
- `StepFinished` after successful steps
- `StepFailed` on non-zero exit or terminal fail-step

Do not change the current `RunnerResult` model.

**Step 3: Extend runner specs**

Assert ordered events for:

- successful execution
- subprocess failure
- terminal `PlanStep.Fail`

**Step 4: Run the runner spec**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.exec.MillRunnerSpec`

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-815-execution-events add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala
git -C .worktrees/mi-815-execution-events commit -m "feat: emit runner step events"
```

### Task 4: Run the verification sweep

**Files:**
- Verify only

**Step 1: Run formatting**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat modules.mill-interceptor.checkFormat`

Expected: PASS

**Step 2: Run test suites**

Run: `./mill --no-server modules.mill-interceptor.test modules.mill-interceptor-maven-plugin.test`

Expected: PASS

**Step 3: Commit final sweep if needed**

```bash
git -C .worktrees/mi-815-execution-events add -A
git -C .worktrees/mi-815-execution-events commit -m "test: verify shared execution events integration"
```
