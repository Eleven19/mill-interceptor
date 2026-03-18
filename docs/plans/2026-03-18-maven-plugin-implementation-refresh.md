# Maven Plugin Implementation Refresh Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the real `mill-interceptor-maven-plugin` with full common Maven lifecycle forwarding, a conventional lifecycle baseline inside strict-mode resolution, and deterministic YAML/PKL customization.

**Architecture:** Keep the published Maven plugin module as the execution boundary, but implement the real plugin as a layered system: lifecycle Mojos at the boundary, config discovery and PKL-backed composition in the middle, a resolver that starts from a conventional lifecycle baseline, and a Kyo-backed external `mill` runner underneath. Treat optional Scalafmt verification as a `validate`-phase hook rather than a separate execution mode.

**Tech Stack:** Mill declarative YAML plus shared Scala build traits in `mill-build/`, Scala 3, Kyo, scribe, Maven plugin API and annotations, a Scala YAML library, PKL JVM runtime, ZIO Test, Maven CLI, beads

---

### Task 1: Refresh The Plugin Goal Surface

**Files:**
- Modify: `mill-build/src/build/Modules.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

**Step 1: Write the failing descriptor test**

Extend `MavenPluginModuleSpec.scala` so it asserts the descriptor contains:

- `describe`
- `inspect-plan`
- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `install`
- `deploy`

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the descriptor still only exposes the placeholder surface.

**Step 3: Implement the goal registry**

Add a typed goal registry in `MavenPluginModule.scala` and drive descriptor
generation from it in `Modules.scala`.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add mill-build/src/build/Modules.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala
git commit -m "Add full Maven plugin goal registry"
```

### Task 2: Add Deterministic Config Discovery

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscovery.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigSource.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscoverySpec.scala`

**Step 1: Write the failing config-discovery test**

Create `ConfigDiscoverySpec.scala` asserting the exact eight-entry discovery
order for repo and module YAML/PKL config files.

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL because the discovery model does not exist.

**Step 3: Implement discovery and build deps**

Add the needed dependency wiring in `package.mill.yaml` and implement ordered
discovery using `os-lib`.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/package.mill.yaml modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscoverySpec.scala
git commit -m "Add Maven plugin config discovery"
```

### Task 3: Add YAML Loading And PKL Composition

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/EffectiveConfig.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoader.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoaderSpec.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/resources/config/`

**Step 1: Write the failing loader tests**

Cover:

- YAML-only config
- PKL amend over YAML
- module override over repo default
- malformed config with source-aware diagnostics

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL.

**Step 3: Implement the minimal config loader**

Use a Scala YAML library for YAML parsing, PKL for final composition, and keep
the output limited to the fields needed for lifecycle resolution and execution.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoaderSpec.scala modules/mill-interceptor-maven-plugin/test/resources/config
git commit -m "Add Maven plugin config loading"
```

### Task 4: Add The Conventional Lifecycle Baseline

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/ExecutionMode.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/LifecycleBaseline.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/model/LifecycleBaselineSpec.scala`

**Step 1: Write the failing baseline tests**

Cover:

- default lifecycle mappings for `clean` through `deploy`
- baseline merge without explicit override duplication
- optional `validate` Scalafmt hook disabled by override

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL.

**Step 3: Implement the baseline model**

Add a conventional lifecycle seed that strict resolution starts from, without
introducing a separate user-facing conventional mode.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/model/LifecycleBaselineSpec.scala
git commit -m "Add Maven lifecycle baseline defaults"
```

### Task 5: Add `MillExecutionPlan` And The Forwarding Resolver

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MavenExecutionRequest.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MillExecutionPlan.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/ModuleIdentity.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolver.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolverSpec.scala`

**Step 1: Write the failing resolver tests**

Cover:

- full lifecycle phase resolution
- explicit goal resolution
- module-local override behavior
- strict failure messaging
- optional Scalafmt hook planning in `validate`

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL.

**Step 3: Implement the minimal resolver**

Produce a pure `MillExecutionPlan` from the effective config and the Maven
request. Keep the plan module-local for now and leave `MI-szk` open for the
later extraction decision.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolverSpec.scala
git commit -m "Add Maven forwarding resolver"
```

### Task 6: Implement The Kyo-Backed Mill Runner

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunResult.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/service/ForwardingService.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/resources/fake-mill/`

**Step 1: Write the failing runner tests**

Cover:

- argument-vector execution
- env forwarding
- stdout and stderr capture
- non-zero exit handling
- dry-run and inspect-plan behavior
- Scalafmt target probing through `mill resolve __`

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL.

**Step 3: Implement the Kyo execution layer**

Use Kyo for command, process, system, and error orchestration. Keep Maven
exception mapping out of this task.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/service modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala modules/mill-interceptor-maven-plugin/test/resources/fake-mill
git commit -m "Add Kyo-based Maven plugin runner"
```

### Task 7: Replace Placeholder Mojos With Full Lifecycle Mojos

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/CleanMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/ValidateMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/CompileMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/TestMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/PackageMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/VerifyMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InstallMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/DeployMojo.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/DescribeMojo.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/ForwardingMojoSpec.scala`

**Step 1: Write the failing Mojo tests**

Cover:

- lifecycle goal annotations
- Maven request capture
- strict error mapping
- inspect-plan rendering
- validate-phase Scalafmt disable flag/property behavior

**Step 2: Run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: FAIL.

**Step 3: Implement the Mojo boundary**

Add `AbstractForwardingMojo` and the full lifecycle Mojo set, delegating to the
service layer.

**Step 4: Re-run the module test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/ForwardingMojoSpec.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala mill-build/src/build/Modules.scala
git commit -m "Add Maven lifecycle forwarding mojos"
```

### Task 8: Add Full Integration Fixtures And Documentation

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/`
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`
- Modify: `README.md`
- Create: `docs/contributing/maven-plugin-config.md`

**Step 1: Write the failing integration tests**

Cover:

- repo-only minimal config
- module override behavior
- full lifecycle phase forwarding
- strict failure path
- inspect-plan output
- optional Scalafmt validate hook enabled and disabled

**Step 2: Run the integration target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`
Expected: FAIL.

**Step 3: Implement only what the fixtures require**

Close the remaining behavior gaps exposed by the end-to-end Maven fixtures.

**Step 4: Re-run verification**

Run:

- `./mill --no-server __.checkFormat`
- `./mill --no-server __.test`
- `./mill --no-server modules.mill-interceptor.itest.testLocal`
- `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`
- `bash scripts/ci/test-publish-central.sh`
- `scripts/ci/test-publish-metadata.sh 1.2.3`
- `bash scripts/ci/test-release-workflows.sh`

Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/itest README.md docs/contributing/maven-plugin-config.md modules/mill-interceptor-maven-plugin/src modules/mill-interceptor-maven-plugin/test
git commit -m "Implement Maven lifecycle forwarding plugin"
```

### Task 9: Final Sync And Tracker Hygiene

**Files:**
- Modify: none unless verification exposes new gaps

**Step 1: Update issue state**

- close the implementation epic only after the full lifecycle behavior lands
- leave `MI-szk` open
- create any newly discovered work with `discovered-from` links

**Step 2: Sync tracker and git**

Run:

```bash
git pull --rebase
bd dolt push --json
git push
git status -sb
```

Expected:

- tracker sync is either clean or any conflict is resolved explicitly
- branch push succeeds
- `git status -sb` is clean and up to date
