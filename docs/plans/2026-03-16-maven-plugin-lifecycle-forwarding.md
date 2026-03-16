# Maven Plugin Lifecycle Forwarding Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the real `mill-interceptor-maven-plugin` so configured Maven lifecycle phases and explicit goals resolve into external `mill` CLI invocations, using deterministic YAML and PKL configuration with strict mode as the default.

**Architecture:** Keep the existing published Maven plugin module in `modules/mill-interceptor-maven-plugin`, but replace the placeholder-only behavior with a layered design: Maven Mojos at the boundary, a shared domain and planning layer in the middle, and a Kyo-backed execution service at the bottom. Treat PKL as the canonical composition layer, keep YAML as a data input format, and implement lifecycle forwarding incrementally behind tests so strict-mode failures, config precedence, and process invocation all stay explicit and verifiable.

**Tech Stack:** Mill declarative YAML plus shared Scala build traits in `mill-build/`, Scala 3, Kyo, scribe, Maven plugin API and annotations, YAML parsing library, PKL JVM runtime, ZIO Test, Maven CLI, beads

---

### Task 1: Replace The Placeholder Descriptor Model With A Real Goal Registry

**Files:**
- Modify: `mill-build/src/build/Modules.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

**Step 1: Write the failing descriptor test**

Extend `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`
so it asserts the plugin exposes a typed registry of supported goals rather than
only the current `describe` placeholder constants. At minimum, assert the
registry names:

- `describe`
- `inspect-plan`
- one lifecycle-bound goal such as `compile`

Also assert the packaged descriptor resource contains all three goal names.

**Step 2: Run the plugin test target to verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because the plugin module and generated descriptor still expose
only the single placeholder goal.

**Step 3: Add the minimal goal registry**

Implement a typed goal registry in
`modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala`,
for example:

```scala
object MavenPluginModule:
  final case class GoalDescriptor(
      goal: String,
      implementationClass: String,
      description: String
  )

  val goalPrefix = "mill-interceptor"
  val supportedGoals = Seq(
    GoalDescriptor("describe", "...DescribeMojo", "..."),
    GoalDescriptor("inspect-plan", "...InspectPlanMojo", "..."),
    GoalDescriptor("compile", "...CompileMojo", "...")
  )
```

Update `mill-build/src/build/Modules.scala` so the generated Maven descriptor
is driven from this registry instead of hard-coded XML for a single Mojo.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for the descriptor-registry assertions.

**Step 5: Commit**

```bash
git add mill-build/src/build/Modules.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala
git commit -m "Add Maven plugin goal registry"
```

### Task 2: Add Config Discovery And Canonical Config Inputs

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscovery.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigSource.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/RawConfigInput.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscoverySpec.scala`

**Step 1: Write the failing config-discovery test**

Create `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscoverySpec.scala`
with a fixture matrix that asserts discovery order is exactly:

1. repo `mill-interceptor.yaml`
2. repo `mill-interceptor.pkl`
3. repo `.config/mill-interceptor/config.yaml`
4. repo `.config/mill-interceptor/config.pkl`
5. module `mill-interceptor.yaml`
6. module `mill-interceptor.pkl`
7. module `.config/mill-interceptor/config.yaml`
8. module `.config/mill-interceptor/config.pkl`

Assert the test receives an ordered list of `ConfigSource` values carrying both
scope and format.

**Step 2: Run the plugin test target to verify red**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because the config discovery types and logic do not exist yet.

**Step 3: Add the minimal build deps and discovery implementation**

Update `modules/mill-interceptor-maven-plugin/package.mill.yaml` with the JVM
dependencies needed for:

- filesystem traversal
- YAML parsing
- PKL evaluation
- Kyo process and system modules if they are packaged separately from
  `kyo-core`

Implement:

```scala
enum ConfigFormat:
  case Yaml, Pkl

enum ConfigScope:
  case Repository, Module

final case class ConfigSource(path: os.Path, scope: ConfigScope, format: ConfigFormat)
```

and a `ConfigDiscovery.discover(repoRoot: os.Path, moduleDir: os.Path): Seq[ConfigSource]`
that returns existing files in the required order.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for config discovery.

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
- Create: `modules/mill-interceptor-maven-plugin/test/resources/config/repo-module-layering/`

**Step 1: Write the failing config-loader tests**

Create `ConfigLoaderSpec.scala` with fixtures that prove:

- YAML-only config produces a usable `EffectiveConfig`
- PKL can amend YAML-derived values
- module PKL overrides repo YAML
- conflicting or malformed config produces a validation error that names the
  source file

Model the test around a minimal canonical config like:

```scala
final case class EffectiveConfig(
    mode: ExecutionMode,
    maven: MavenConfig,
    mill: MillConfig
)
```

with only enough fields to prove precedence and strict-mode defaults.

**Step 2: Run the config-loader spec and verify it fails**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because YAML parsing, PKL evaluation, and validation do not
exist yet.

**Step 3: Implement the minimal loader and evaluator**

Implement:

- YAML parsing into canonical raw config inputs
- PKL evaluation that receives YAML-derived values as its base input
- final schema validation into `EffectiveConfig`

Do not add lifecycle resolution yet. Keep this task focused on config loading
and precedence.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for config precedence and validation.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoaderSpec.scala modules/mill-interceptor-maven-plugin/test/resources/config/repo-module-layering
git commit -m "Add YAML and PKL config loading"
```

### Task 4: Introduce The Shared Forwarding Domain And `MillExecutionPlan`

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MavenExecutionRequest.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MillExecutionPlan.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/ExecutionMode.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/ModuleIdentity.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolver.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolverSpec.scala`

**Step 1: Write the failing resolver tests**

Create `ForwardingResolverSpec.scala` with cases for:

- `compile` phase maps to the configured Mill task list
- `test` phase picks up a module-local override
- explicit mapped goal resolves correctly
- strict mode fails with a message that names the missing phase or goal and the
  consulted config sources

Assert against `MillExecutionPlan` rather than raw strings.

**Step 2: Run the plugin test target to verify red**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because the resolver and `MillExecutionPlan` do not exist yet.

**Step 3: Add the minimal domain model and resolver**

Implement a pure resolution layer, for example:

```scala
final case class MillExecutionPlan(
    request: MavenExecutionRequest,
    workingDirectory: os.Path,
    executable: String,
    args: Seq[String],
    forwardedProperties: Map[String, String]
)
```

Keep it independent from Mojo classes and process execution. Do not extract it
out of the module in this task; leave that follow-up to bead `MI-szk`.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for phase and goal resolution.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ForwardingResolverSpec.scala
git commit -m "Add Maven forwarding resolution model"
```

### Task 5: Implement The Kyo-Backed Mill Runner

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunResult.scala`
- Create: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/service/ForwardingService.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`
- Create: `modules/mill-interceptor-maven-plugin/test/resources/fake-mill/`

**Step 1: Write the failing runner tests**

Create `MillRunnerSpec.scala` with a fixture executable that proves:

- the command is executed as an argument vector, not a shell string
- configured environment variables are forwarded
- stdout and stderr are both captured for diagnostics
- non-zero exit status becomes a structured failure result
- dry-run returns the resolved `MillExecutionPlan` without executing the
  process

Use a temporary fake executable under `test/resources/fake-mill/` instead of
calling the real `mill` binary.

**Step 2: Run the plugin test target to verify red**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because the Kyo-backed runner and service do not exist yet.

**Step 3: Implement the minimal Kyo execution layer**

Use Kyo abstractions for process, command, system, and structured effects.
`ForwardingService` should orchestrate:

1. config discovery and loading
2. resolver invocation
3. dry-run or real process execution
4. structured success or failure mapping

Keep Maven-specific exception translation out of this task.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for process execution behavior.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/service modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala modules/mill-interceptor-maven-plugin/test/resources/fake-mill
git commit -m "Add Kyo-based mill execution service"
```

### Task 6: Replace The Placeholder Mojos With Lifecycle And Inspect Entry Points

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

Create `ForwardingMojoSpec.scala` that asserts:

- each lifecycle Mojo advertises the expected goal name
- the Mojo layer translates Maven context into a `MavenExecutionRequest`
- strict-mode failures are converted into Maven-friendly exceptions
- `InspectPlanMojo` renders the resolved plan without executing the process

Keep the tests narrow by mocking or faking the service layer instead of running
the real process.

**Step 2: Run the plugin test target to verify red**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: FAIL because the lifecycle and inspect Mojos do not exist yet.

**Step 3: Implement the Mojo boundary**

Introduce a shared `AbstractForwardingMojo` that:

- extracts Maven session, project, basedir, profiles, and system properties
- constructs a `MavenExecutionRequest`
- delegates to `ForwardingService`
- maps failures to plugin exceptions with useful error messages

Keep `DescribeMojo` as a lightweight informational goal if still useful, but do
not use it as the main forwarding path anymore.

**Step 4: Re-run the plugin test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`

Expected: PASS for Mojo behavior.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/ForwardingMojoSpec.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModule.scala mill-build/src/build/Modules.scala
git commit -m "Add Maven lifecycle forwarding mojos"
```

### Task 7: Add End-To-End Maven Fixture Coverage For YAML And PKL

**Files:**
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/lifecycle-repo-yaml/`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/module-pkl-override/`
- Create: `modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-mode-failure/`
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml` if additional itest resource or env wiring is needed

**Step 1: Write the failing integration tests**

Extend `MavenPluginIntegrationSpec.scala` so it runs Maven against fixture
projects that prove:

- repo-level YAML forwards `mvn compile` to the configured Mill command
- module-level PKL overrides the repo-level mapping
- strict mode fails on an unmapped phase with a clear message
- `inspect-plan` prints the resolved `MillExecutionPlan`

Use a fake `mill` executable path defined in the fixture config so the tests
assert invocation shape without depending on a real Mill build.

**Step 2: Run the plugin integration test target to verify red**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`

Expected: FAIL because the forwarding implementation and new fixtures are not
fully wired yet.

**Step 3: Implement only what the integration tests require**

Fill any gaps in config loading, request translation, or process execution that
the integration suite exposes. Keep the behavior within the approved scope:

- lifecycle forwarding
- explicit inspect goal
- strict-mode failure
- YAML and PKL layering

**Step 4: Re-run the plugin integration test target**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`

Expected: PASS.

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/itest modules/mill-interceptor-maven-plugin/package.mill.yaml modules/mill-interceptor-maven-plugin/src
git commit -m "Add Maven lifecycle forwarding integration tests"
```

### Task 8: Document The Config Surface And Finish Verification

**Files:**
- Modify: `docs/contributing/releasing.md` only if publish behavior changed
- Create: `docs/contributing/maven-plugin-config.md`
- Modify: `README.md` if Maven plugin usage belongs in the main project overview
- Modify: CI files only if verification exposes missing coverage

**Step 1: Write the failing documentation expectations**

Add or update docs so they describe:

- supported config file names and locations
- strict mode as the default
- YAML as input and PKL as composition
- the current supported lifecycle goals and inspect goals

Do not document hybrid mode or unsupported third-party plugin-goal behavior as
available.

**Step 2: Run the full verification set**

Run:

- `./mill --no-server __.checkFormat`
- `./mill --no-server __.test`
- `./mill --no-server modules.mill-interceptor.itest.testLocal`
- `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`
- `bash scripts/ci/test-publish-central.sh`
- `scripts/ci/test-publish-metadata.sh 1.2.3`
- `bash scripts/ci/test-release-workflows.sh`

Expected: all PASS.

**Step 3: Request review and log follow-up boundaries**

Before merging, keep bead `MI-szk` open for the shared `MillExecutionPlan`
follow-up. If implementation reveals more deferred work, create additional
beads linked back to the execution issue rather than adding markdown TODOs.

**Step 4: Commit**

```bash
git add docs/contributing/maven-plugin-config.md README.md modules/mill-interceptor-maven-plugin .github/workflows scripts/ci
git commit -m "Document Maven lifecycle forwarding plugin"
```

### Task 9: Final Sync And Delivery

**Files:**
- Modify: none unless final verification exposes a gap

**Step 1: Repeat fresh verification**

Run the full verification set from Task 8 again immediately before claiming
completion.

**Step 2: Update issue state**

- close the execution issue once the implementation lands
- leave `MI-szk` open
- create any new discovered work with `discovered-from` dependencies

**Step 3: Sync tracker and git**

Run:

```bash
git pull --rebase
bd dolt push --json
git push
git status -sb
```

Expected:

- tracker push succeeds
- branch push succeeds
- `git status -sb` shows the branch clean and up to date with `origin`
