# Maven Lifecycle Mojos Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the placeholder Maven plugin surface with real lifecycle Mojos and a shared Maven adapter that resolves and executes Mill plans for the common lifecycle.

**Architecture:** Keep the existing resolver and runner intact, and add a thin Maven boundary around them. Real per-phase Mojos should delegate to one shared adapter that translates Maven state into `ExecutionRequest`, resolves plans, executes them, and renders `inspect-plan`.

**Tech Stack:** Scala 3, Mill declarative build, Maven plugin annotations/APIs, Kyo, zio-test, scribe

---

### Task 1: Add the shared Maven adapter model

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MavenExecutionContext.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojoSpec.scala`

**Step 1: Write the failing test**

Add tests that prove the shared adapter can:
- build a neutral `ExecutionRequest` from Maven-like context
- identify lifecycle-phase versus explicit-goal execution
- preserve repo root, module root, artifact id, packaging, and user properties

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.mojo.AbstractForwardingMojoSpec`

Expected: FAIL because the shared adapter types do not exist yet.

**Step 3: Write minimal implementation**

Add the shared context type and abstract Mojo base with narrow overridable hooks
for:
- requested execution name
- execution kind
- execute-versus-inspect behavior

Keep direct Maven wiring thin and isolated.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.mojo.AbstractForwardingMojoSpec`

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-7-lifecycle-mojos add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MavenExecutionContext.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojoSpec.scala
git -C .worktrees/mi-okw-7-lifecycle-mojos commit -m "feat: add shared maven forwarding mojo adapter"
```

### Task 2: Replace placeholder lifecycle goals with real Mojos

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/CleanMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/ValidateMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/CompileMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/TestMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/PackageMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/VerifyMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InstallMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/DeployMojo.scala`
- Modify: `modules/mill-interceptor-maven-plugin/resources/maven-plugin-goals.tsv`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

**Step 1: Write the failing test**

Extend `MavenPluginModuleSpec` to assert that the goal registry maps the common
lifecycle goals to distinct concrete Mojo classes rather than `DescribeMojo`.

**Step 2: Run test to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginModuleSpec`

Expected: FAIL because the registry still points at placeholder Mojos.

**Step 3: Write minimal implementation**

Add the concrete lifecycle Mojo classes as tiny subclasses of the shared
adapter, then update the goal registry resource to reference them.

**Step 4: Run test to verify it passes**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginModuleSpec`

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-7-lifecycle-mojos add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo modules/mill-interceptor-maven-plugin/resources/maven-plugin-goals.tsv modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala
git -C .worktrees/mi-okw-7-lifecycle-mojos commit -m "feat: add real lifecycle mojos"
```

### Task 3: Add inspect-plan and failure translation behavior

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojo.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojoSpec.scala`
- Test: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MojoFailureMappingSpec.scala`

**Step 1: Write the failing tests**

Add tests that prove:
- `inspect-plan` renders the ordered dry-run plan without executing subprocesses
- fail-step, probe-failure, and invocation-failure outcomes become Maven-friendly
  exceptions with guidance preserved

**Step 2: Run tests to verify they fail**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.mojo.InspectPlanMojoSpec io.eleven19.mill.interceptor.maven.plugin.mojo.MojoFailureMappingSpec`

Expected: FAIL because inspect-plan and exception mapping are not implemented.

**Step 3: Write minimal implementation**

Add the operational Mojo, wire it through the shared adapter, and map runner or
plan failures to Maven exceptions with useful messages.

**Step 4: Run tests to verify they pass**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.mojo.InspectPlanMojoSpec io.eleven19.mill.interceptor.maven.plugin.mojo.MojoFailureMappingSpec`

Expected: PASS

**Step 5: Commit**

```bash
git -C .worktrees/mi-okw-7-lifecycle-mojos add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojo.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojoSpec.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MojoFailureMappingSpec.scala
git -C .worktrees/mi-okw-7-lifecycle-mojos commit -m "feat: add inspect-plan mojo and failure mapping"
```

### Task 4: Verify the module and close out the task

**Files:**
- Verify only

**Step 1: Run the focused module checks**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.test
./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat
```

Expected: both commands PASS.

**Step 2: Review the task acceptance criteria**

Confirm that:
- the full common lifecycle is represented by Mojos
- the shared Mojo boundary captures request details correctly
- failures are translated into Maven-friendly diagnostics

**Step 3: Update tracker state**

Run:

```bash
bd close MI-okw.7 --reason "Completed" --json
bd dolt push --json
```

Expected: issue closes and tracker state pushes successfully.

**Step 4: Push branch**

Run:

```bash
git -C .worktrees/mi-okw-7-lifecycle-mojos push
git -C .worktrees/mi-okw-7-lifecycle-mojos status -sb
```

Expected: branch is up to date with origin and worktree is clean.

**Step 5: Commit**

No new commit if verification-only. If verification required a final doc or test
adjustment, commit that adjustment before push.
