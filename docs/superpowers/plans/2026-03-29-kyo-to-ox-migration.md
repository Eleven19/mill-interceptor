# Kyo to Ox + PureLogic Migration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Kyo effect system with direct-style Scala 3 using Ox, PureLogic, and os-lib across the entire codebase.

**Architecture:** Two-module codebase migrated sequentially (Maven plugin first, then CLI). Each Kyo effect is replaced with its direct-style equivalent: `kyo.Path` becomes `os.Path`, `Abort[E]` becomes `Either[E, A]`, `Sync.defer` becomes plain calls, `KyoSpecDefault` becomes `ZIOSpecDefault`. PureLogic's `Abort[E]` via context functions is used for typed errors in pure domain logic. Ox provides `OxApp.Simple` for the CLI entry point.

**Tech Stack:** Scala 3.8.2, Ox 1.0.4, PureLogic 0.2.0, os-lib, ZIO Test, Scribe

**Spec:** `docs/superpowers/specs/2026-03-29-kyo-to-ox-migration-design.md`

---

## Translation Reference

Agents executing this plan should apply these mechanical translations. Read each file, apply the relevant patterns, and write it back.

### Import replacements

```scala
// REMOVE all of these:
import kyo.*
import kyo.Path
import kyo.test.KyoSpecDefault

// ADD as needed:
import os.{Path as OsPath, *}  // or just: import os.*
import zio.test.*               // keep existing
// For test files, change base class:
// KyoSpecDefault → ZIOSpecDefault
```

### Path type migration

```scala
// BEFORE                          // AFTER
kyo.Path                           os.Path
Path("string")                     os.Path(java.nio.file.Paths.get("string"))
Path(parent, "child")              parent / "child"
Path(parent, "a", "b")             parent / "a" / "b"
path.exists                        os.exists(path)
path.read                          os.read(path)
path.write(content)                os.write(path, content)
path.mkDir                         os.makeDir.all(path)
path.toJava                        path.toNIO
path.removeAll                     os.remove.all(path)
```

### Effect removal

```scala
// BEFORE                                    // AFTER
def f(): T < Sync = Sync.defer { ... }       def f(): T = { ... }
def f(): T < (Abort[E] & Sync)               def f(): Either[E, T]
Abort.fail(e)                                 Left(e)  // or return early
Abort.run[E](expr)                            expr  // already returns Either
Abort.catching[E] { body }                    try { Right(body) } catch { case e: E => Left(wrap(e)) }
Sync.Unsafe.evalOrThrow(expr)                 expr  // direct call
Kyo.foreach(seq)(f)                           seq.map(f)
Kyo.foldLeft(seq)(init)(f)                    seq.foldLeft(init)(f)
Kyo.unit                                      ()
direct { body }                               body  // remove wrapper
expr.now                                      expr  // remove .now
```

### Test migration

```scala
// BEFORE
object MySpec extends KyoSpecDefault:
    def spec = suite("X")(
        test("y") {
            Sync.defer(assertTrue(1 == 1))
        },
        test("z") {
            Abort.run[E](expr).map { result =>
                assertTrue(result == ...)
            }
        }
    )

// AFTER
object MySpec extends ZIOSpecDefault:
    def spec = suite("X")(
        test("y") {
            assertTrue(1 == 1)
        },
        test("z") {
            val result = expr  // expr now returns Either directly
            assertTrue(result == ...)
        }
    )
```

---

## Phase 1: Setup

### Task 1: Create worktree and branch

- [ ] **Step 1: Create the worktree**

```bash
git worktree add .worktrees/kyo-to-ox --branch refactor/kyo-to-ox-migration
```

- [ ] **Step 2: Switch to the worktree**

All subsequent work happens in `.worktrees/kyo-to-ox`.

- [ ] **Step 3: Commit**

No changes yet — worktree is ready.

---

### Task 2: Update build dependencies

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Modify: `modules/mill-interceptor/package.mill.yaml`

- [ ] **Step 1: Update maven-plugin module dependencies**

In `modules/mill-interceptor-maven-plugin/package.mill.yaml`, replace the Kyo dependencies:

```yaml
# REMOVE these lines:
  - io.getkyo::kyo-core::1.0-RC1
  - io.getkyo::kyo-direct::1.0-RC1
  - io.getkyo::kyo-prelude::1.0-RC1

# ADD:
  - com.github.ghostdogpr::purelogic::0.2.0
```

In the test dependencies section, replace:

```yaml
# REMOVE:
  - io.getkyo::kyo-zio-test::1.0-RC1

# The zio-test deps should already be present; keep them
```

- [ ] **Step 2: Update CLI module dependencies**

In `modules/mill-interceptor/package.mill.yaml`, replace the Kyo dependencies:

```yaml
# REMOVE these lines:
  - io.getkyo::kyo-core::1.0-RC1
  - io.getkyo::kyo-direct::1.0-RC1
  - io.getkyo::kyo-prelude::1.0-RC1

# ADD:
  - com.softwaremill.ox::core::1.0.4
  - com.github.ghostdogpr::purelogic::0.2.0
```

In the test dependencies section, replace:

```yaml
# REMOVE:
  - io.getkyo::kyo-zio-test::1.0-RC1
```

- [ ] **Step 3: Verify the build definition compiles**

```bash
./mill resolve modules.mill-interceptor-maven-plugin.compile
```

This will fail (source files still import kyo) but confirms the build definition is valid.

- [ ] **Step 4: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/package.mill.yaml modules/mill-interceptor/package.mill.yaml
git commit -m "build: replace Kyo dependencies with Ox and PureLogic"
```

---

## Phase 2: Shared Model Types

### Task 3: Migrate MillExecutionPlan.scala (shared models)

This file is in the CLI module but used by both modules. It defines `ExecutionRequest`, `MillExecutionPlan`, `PlanStep`, `ModuleRef`, `ExecutionMode`, `ExecutionRequestKind`, `ExecutionEvent`, `ExecutionEventSink`.

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala`

- [ ] **Step 1: Read the file and identify all kyo.Path usage**

The file imports `import kyo.Path` and uses `Path` in `ExecutionRequest` fields (`repoRoot: Path`, `moduleRoot: Path`).

- [ ] **Step 2: Replace kyo.Path with os.Path**

Replace `import kyo.Path` with `import os.Path`. All `Path` type references now resolve to `os.Path`. No other Kyo types are used in this file.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/MillExecutionPlan.scala
git commit -m "refactor: migrate MillExecutionPlan shared models from kyo.Path to os.Path"
```

---

## Phase 3: Maven Plugin Source Files

Migrate bottom-up: config layer → model → resolve → exec → mojo/extension.

### Task 4: Migrate EffectiveConfig.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/EffectiveConfig.scala`

- [ ] **Step 1: Read the file and identify kyo usage**

Uses `import kyo.Path` only for `ConfigLoadException(path: Path, ...)`. Replace with `os.Path`.

- [ ] **Step 2: Replace import and Path type**

Replace `import kyo.Path` with `import os.Path`. The `ConfigLoadException` case class and all `ConfigOverlay`/`EffectiveConfig` types stay the same — only the `Path` import changes.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/EffectiveConfig.scala
git commit -m "refactor: migrate EffectiveConfig from kyo.Path to os.Path"
```

---

### Task 5: Migrate ConfigDiscovery.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscovery.scala`

- [ ] **Step 1: Read the file and understand current kyo usage**

Uses `import kyo.*`. The `discover` method returns `Seq[DiscoveredConfigSource] < Sync` and uses `Kyo.foreach` with `candidate.path.exists`. The `childPath` helper uses `kyo.Path`.

- [ ] **Step 2: Convert to direct style**

The method signature changes from `Seq[DiscoveredConfigSource] < Sync` to plain `Seq[DiscoveredConfigSource]`. Replace `Kyo.foreach` + `path.exists` with `candidates.filter(c => os.exists(c.path))`. Update `childPath` to use `os.Path`. Update `DiscoveredConfigSource` to use `os.Path`.

```scala
package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path

enum ConfigScope derives CanEqual:
    case Repository
    case Module

enum ConfigFormat derives CanEqual:
    case Yaml
    case Pkl

final case class DiscoveredConfigSource(
    scope: ConfigScope,
    format: ConfigFormat,
    path: Path
) derives CanEqual

object ConfigDiscovery:

    private def childPath(parent: Path, children: String*): Path =
        os.Path(children.foldLeft(parent.toNIO)((p, c) => p.resolve(c)))

    def discover(repoRoot: Path, moduleRoot: Path): Seq[DiscoveredConfigSource] =
        val candidates = Seq(
            DiscoveredConfigSource(ConfigScope.Repository, ConfigFormat.Yaml, childPath(repoRoot, "mill-interceptor.yaml")),
            DiscoveredConfigSource(ConfigScope.Repository, ConfigFormat.Pkl, childPath(repoRoot, "mill-interceptor.pkl")),
            DiscoveredConfigSource(ConfigScope.Repository, ConfigFormat.Yaml, childPath(repoRoot, ".config", "mill-interceptor", "config.yaml")),
            DiscoveredConfigSource(ConfigScope.Repository, ConfigFormat.Pkl, childPath(repoRoot, ".config", "mill-interceptor", "config.pkl")),
            DiscoveredConfigSource(ConfigScope.Module, ConfigFormat.Yaml, childPath(moduleRoot, "mill-interceptor.yaml")),
            DiscoveredConfigSource(ConfigScope.Module, ConfigFormat.Pkl, childPath(moduleRoot, "mill-interceptor.pkl")),
            DiscoveredConfigSource(ConfigScope.Module, ConfigFormat.Yaml, childPath(moduleRoot, ".config", "mill-interceptor", "config.yaml")),
            DiscoveredConfigSource(ConfigScope.Module, ConfigFormat.Pkl, childPath(moduleRoot, ".config", "mill-interceptor", "config.pkl"))
        )
        candidates.filter(c => os.exists(c.path))
```

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscovery.scala
git commit -m "refactor: migrate ConfigDiscovery from Kyo effects to direct style with os.Path"
```

---

### Task 6: Migrate PklConfigEvaluator.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala`

- [ ] **Step 1: Read the file and understand kyo usage**

Uses `Abort.catching[ConfigLoadException]` and `Sync.defer` to wrap PKL evaluation. Returns `ConfigOverlay < (Abort[ConfigLoadException] & Sync)`.

- [ ] **Step 2: Convert to direct style returning Either**

The method should return `Either[ConfigLoadException, ConfigOverlay]`. Replace the Kyo effect wrappers with a try/catch that returns `Either`.

```scala
package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path
import org.pkl.core.{Evaluator, ModuleSource, PModule, PObject}
import scala.jdk.CollectionConverters.*

object PklConfigEvaluator:

    def load(path: Path): Either[ConfigLoadException, ConfigOverlay] =
        try
            val evaluator = Evaluator.preconfigured()
            try
                val module = evaluator.evaluate(ModuleSource.path(path.toNIO))
                Right(ConfigOverlay.fromRawMap(toRawMap(module), path))
            finally evaluator.close()
        catch
            case error: ConfigLoadException => Left(error)
            case error: Throwable => Left(ConfigLoadException(path, error.getMessage.nn, error))

    private def toRawMap(module: PModule): Map[String, Any] =
        module.getProperties.asScala.toMap.view.mapValues(convertValue).toMap

    private def convertValue(value: Any): Any =
        value match
            case module: PModule => module.getProperties.asScala.toMap.view.mapValues(convertValue).toMap
            case obj: PObject => obj.getProperties.asScala.toMap.view.mapValues(convertValue).toMap
            case map: java.util.Map[?, ?] => map.asScala.toMap.collect { case (key: String, nested) => key -> convertValue(nested) }
            case list: java.util.List[?] => list.asScala.toSeq.map(convertValue)
            case other => other
```

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala
git commit -m "refactor: migrate PklConfigEvaluator from Kyo effects to Either"
```

---

### Task 7: Migrate ConfigLoader.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoader.scala`

- [ ] **Step 1: Read the file and understand kyo usage**

Uses `Kyo.foldLeft` over discovered sources, `Abort[ConfigLoadException] & Sync` effect. Calls `path.read` (Kyo Path) and `content.as[ConfigOverlay]` (scala-yaml).

- [ ] **Step 2: Convert to direct style returning Either**

Replace Kyo effects with direct calls. `ConfigDiscovery.discover` is now a plain method. `loadOverlay` returns `Either`. Use `foldLeft` with `Either` short-circuiting via a `flatMap` chain or a simple imperative loop.

```scala
package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path
import org.virtuslab.yaml.*

object ConfigLoader:

    def load(repoRoot: Path, moduleRoot: Path): Either[ConfigLoadException, EffectiveConfig] =
        val discovered = ConfigDiscovery.discover(repoRoot, moduleRoot)
        discovered.foldLeft[Either[ConfigLoadException, ConfigOverlay]](Right(ConfigOverlay())) {
            case (Right(current), source) =>
                loadOverlay(source).map(current.merge)
            case (left, _) => left
        }.map(_.toEffectiveConfig)

    private def loadOverlay(source: DiscoveredConfigSource): Either[ConfigLoadException, ConfigOverlay] =
        source.format match
            case ConfigFormat.Yaml => loadYaml(source.path)
            case ConfigFormat.Pkl  => PklConfigEvaluator.load(source.path)

    private def loadYaml(path: Path): Either[ConfigLoadException, ConfigOverlay] =
        try
            val content = os.read(path)
            content.as[ConfigOverlay] match
                case Right(decoded) => Right(decoded)
                case Left(error)    => Left(ConfigLoadException(path, error.toString))
        catch
            case error: Throwable => Left(ConfigLoadException(path, error.getMessage.nn, error))
```

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoader.scala
git commit -m "refactor: migrate ConfigLoader from Kyo effects to Either"
```

---

### Task 8: Migrate MillRunner.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`

- [ ] **Step 1: Read the full file**

This is the largest file (~270 lines). Uses `kyo.Path`, `Sync`, `Kyo.foreach`, `Process.Command`. The `SubprocessExecutor` trait returns `Int < Sync`. Methods like `execute`, `resolveExecutable`, `executeSteps` all use Kyo effects.

- [ ] **Step 2: Convert all types and effects**

Key changes:
- `SubprocessExecutor.run` returns `Int` (plain) instead of `Int < Sync`
- `execute` returns `RunnerResult` instead of `RunnerResult < Sync`
- `resolveExecutable` returns `String` instead of `String < Sync`
- `executeSteps` returns `RunnerResult` instead of `RunnerResult < Sync`
- Replace `path.exists` with `os.exists(path)`
- Replace `Kyo.foreach` with `.map`
- Replace `Process.Command(...)` with `os.proc(...).call(...)`
- Replace `Sync.defer { ... }` with `{ ... }`
- All `kyo.Path` becomes `os.Path`

The `SubprocessExecutor.live` implementation uses `Process.Command`. Replace with:

```scala
object SubprocessExecutor:
    val live: SubprocessExecutor = new SubprocessExecutor:
        def run(command: Seq[String], workingDirectory: Path, environment: Map[String, String]): Int =
            val result = os.proc(command).call(
                cwd = workingDirectory,
                env = environment,
                stdin = os.Inherit,
                stdout = os.Inherit,
                stderr = os.Inherit,
                check = false
            )
            result.exitCode
```

For `resolveExecutable`, replace `Kyo.foreach` + `.exists` with:

```scala
private def resolveExecutable(request: ExecutionRequest, config: EffectiveConfig): String =
    if config.mill.executable != "mill" then config.mill.executable
    else
        val launcherCandidates = Seq(
            request.moduleRoot / "mill",
            request.moduleRoot / "millw",
            request.repoRoot / "mill",
            request.repoRoot / "millw"
        )
        launcherCandidates.find(os.exists).map(_.toString).getOrElse("mill")
```

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala
git commit -m "refactor: migrate MillRunner from Kyo effects to direct style with os-lib"
```

---

### Task 9: Migrate MavenExecutionContext.scala and LifecycleInterceptionAdapter.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MavenExecutionContext.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/extension/LifecycleInterceptionAdapter.scala`

- [ ] **Step 1: Replace kyo.Path imports with os.Path**

Both files only use `kyo.Path` for type references (`repoRoot: Path`, `moduleRoot: Path`). Replace `import kyo.Path` with `import os.Path`. No effect changes needed.

- [ ] **Step 2: Update Path construction in LifecycleInterceptionAdapter**

The adapter creates `Path(string)`. Replace with `os.Path(java.nio.file.Paths.get(string))` or add a helper.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MavenExecutionContext.scala \
      modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/extension/LifecycleInterceptionAdapter.scala
git commit -m "refactor: migrate MavenExecutionContext and LifecycleInterceptionAdapter to os.Path"
```

---

### Task 10: Migrate AbstractForwardingMojo.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala`

- [ ] **Step 1: Read and understand the Kyo boundary**

This file is the critical integration point. It uses `Sync.Unsafe.evalOrThrow(Abort.run[ConfigLoadException](...))` to bridge Kyo effects into Maven's synchronous execution model. With direct style, this entire bridge disappears.

- [ ] **Step 2: Replace loadEffectiveConfig**

The `loadEffectiveConfig` method becomes trivial:

```scala
protected def loadEffectiveConfig(): EffectiveConfig =
    ConfigLoader.load(executionContext.repoRoot, executionContext.moduleRoot) match
        case Right(config) => config
        case Left(error)   => throw MojoExecutionException(error.getMessage, error)
```

Remove `AllowUnsafe`, `Sync.Unsafe`, `Abort.run`, `Result.Success/Error/Panic` imports and usage.

- [ ] **Step 3: Replace executeResolvedPlan**

The `executeResolvedPlan` method also drops Kyo wrappers:

```scala
protected def executeResolvedPlan(plan: MillExecutionPlan, config: EffectiveConfig): RunnerResult =
    MillRunner.execute(plan, config)
```

Remove `AllowUnsafe` and `Sync.Unsafe.evalOrThrow` wrapper.

- [ ] **Step 4: Update Path construction in normalizedPath and executionContext**

Replace `Path(string)` with `os.Path(java.nio.file.Paths.get(string))`. Replace `import kyo.*` with `import os.Path`.

- [ ] **Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojo.scala
git commit -m "refactor: remove Kyo effect boundary from AbstractForwardingMojo"
```

---

### Task 11: Migrate MojoDescriptorMeta.scala

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MojoDescriptorMeta.scala`

- [ ] **Step 1: Check for kyo imports**

This file may not have kyo imports (it was recently created). If it does, replace `kyo.Path` references with `os.Path`.

- [ ] **Step 2: Commit if changed**

```bash
git add modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/MojoDescriptorMeta.scala
git commit -m "refactor: ensure MojoDescriptorMeta uses os.Path"
```

---

### Task 12: Verify maven-plugin source compiles

- [ ] **Step 1: Compile the maven-plugin module**

```bash
./mill modules.mill-interceptor-maven-plugin.compile
```

Expected: Compilation succeeds. If it fails, fix the errors — they will be missing imports, type mismatches from `Path`, or leftover Kyo references.

- [ ] **Step 2: Fix any compilation errors and commit**

---

## Phase 4: Maven Plugin Tests

### Task 13: Migrate maven-plugin unit tests

All test files follow the same pattern: replace `KyoSpecDefault` with `ZIOSpecDefault`, remove `Sync.defer` wrappers, replace `kyo.Path` with `os.Path`, replace Kyo effect evaluations with direct calls.

**Files (15 test files):**
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigDiscoverySpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoaderSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunnerResultSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/model/MillExecutionPlanSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/model/LifecycleBaselineSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/AbstractForwardingMojoSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/InspectPlanMojoSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/mojo/MojoFailureMappingSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/extension/MillInterceptorLifecycleParticipantSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/extension/LifecycleInterceptionAdapterSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolverSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MojoDescriptorMetaSpec.scala`
- Modify: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginModuleSpec.scala`

- [ ] **Step 1: Apply mechanical translations to ALL test files**

For each file:
1. Replace `import kyo.*` / `import kyo.Path` / `import kyo.test.KyoSpecDefault` with `import os.Path` and `import zio.test.*`
2. Replace `extends KyoSpecDefault` with `extends ZIOSpecDefault`
3. Remove `Sync.defer(...)` wrappers — just keep the inner expression
4. Replace `Abort.run[E](expr).map { ... }` with direct `Either` matching
5. Replace `Path(...)` constructors with `os.Path(...)` equivalents
6. Replace `path.exists` with `os.exists(path)`
7. For MillRunnerSpec: the `RecordingExecutor.run` return type changes from `Int < Sync` to plain `Int`. Remove `Sync.defer { ... }` wrapper.
8. For MillRunnerSpec: `childPath` helper uses `os.Path(base.toNIO.resolve(child))` instead of `Path(base.toJava.resolve(child).toString)`
9. For ConfigLoaderSpec/ConfigDiscoverySpec: `path.write(...)` becomes `os.write(path, ...)`, `path.removeAll` becomes `os.remove.all(path)`, etc.

- [ ] **Step 2: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/test/
git commit -m "refactor: migrate all maven-plugin unit tests from KyoSpecDefault to ZIOSpecDefault"
```

---

### Task 14: Migrate maven-plugin integration tests

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

- [ ] **Step 1: Read the file and understand kyo usage**

Uses `KyoSpecDefault`, `Sync`, `Path`, `Process.Command` for running Maven, `Kyo.foreach`. The `runCommand` method uses `Process.Command(...).spawn`, `process.stdout.readAllBytes()`, `process.waitFor`.

- [ ] **Step 2: Convert to direct style**

Replace `KyoSpecDefault` with `ZIOSpecDefault`. Replace Kyo `Path` with `os.Path`. Replace `Process.Command` with `os.proc`. Replace `Sync.defer` with direct calls. The `runCommand` method becomes:

```scala
private def runCommand(command: Seq[String], cwd: os.Path): CommandResult =
    val result = os.proc(command).call(
        cwd = cwd,
        mergeErrIntoOut = true,
        check = false
    )
    CommandResult(result.exitCode, result.out.text())
```

Replace `copyFixtureDirectory` to use `os.copy` instead of `Files.walkFileTree`. Replace `installRepoMillLauncher` to use `os.copy` and `os.perms.set`. Replace `tempPath` to use `os.Path`. Replace `pathExists` with `os.exists`.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/itest/
git commit -m "refactor: migrate integration tests from Kyo to direct style with os-lib"
```

---

## Phase 5: Maven Plugin Verification

### Task 15: Run all maven-plugin tests

- [ ] **Step 1: Run formatting**

```bash
./mill modules.mill-interceptor-maven-plugin.reformat
```

- [ ] **Step 2: Run unit tests**

```bash
./mill modules.mill-interceptor-maven-plugin.test.testForked
```

Expected: All unit tests pass.

- [ ] **Step 3: Run integration tests**

```bash
./mill modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: All 6 integration tests pass.

- [ ] **Step 4: Run format check**

```bash
./mill modules.mill-interceptor-maven-plugin.checkFormat
```

Expected: No formatting issues.

- [ ] **Step 5: Fix any failures, re-run, commit**

- [ ] **Step 6: Push and verify CI**

```bash
git push -u origin refactor/kyo-to-ox-migration
```

Wait for CI to go green before proceeding to Phase 6.

---

## Phase 6: CLI Module Source Files

### Task 16: Remove ScribeLog.scala (Kyo Log bridge)

**Files:**
- Delete: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/ScribeLog.scala`

- [ ] **Step 1: Delete the file**

This file implements `Log.Unsafe` from Kyo to bridge to Scribe. With Kyo removed, Scribe is called directly. Delete the entire file.

- [ ] **Step 2: Commit**

```bash
git rm modules/mill-interceptor/src/io/eleven19/mill/interceptor/ScribeLog.scala
git commit -m "refactor: remove ScribeLog Kyo bridge (use Scribe directly)"
```

---

### Task 17: Migrate Cli.scala

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/Cli.scala`

- [ ] **Step 1: Read and understand kyo usage**

Uses `Abort.fail[IllegalArgumentException]` for parse errors. Returns types like `CliCommand < Abort[IllegalArgumentException]`. Uses `Chunk` from Kyo.

- [ ] **Step 2: Convert to Either-returning functions**

Replace `Abort.fail(new IllegalArgumentException(...))` with `Left(new IllegalArgumentException(...))`. Replace `Chunk` with `Seq` or `List`. All parse methods return `Either[IllegalArgumentException, T]`.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/Cli.scala
git commit -m "refactor: migrate Cli from Kyo Abort to Either"
```

---

### Task 18: Migrate build tool runners (Mvn, Gradle, Sbt)

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/maven/Mvn.scala`
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/gradle/Gradle.scala`
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/sbt/Sbt.scala`

- [ ] **Step 1: Read all three files**

All three follow the same pattern: `direct { ... }` block with `Process.Command(...)`, `.now` calls, `Log.info(...)`.

- [ ] **Step 2: Convert all three to direct style**

Remove `direct { ... }` wrapper. Remove `.now` calls. Replace `Process.Command(args*).stdin(Inherit).stdout(Inherit).stderr(Inherit).waitFor` with `os.proc(args).call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit, check = false).exitCode`. Replace `Log.info(...)` with `scribe.info(...)`.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/maven/Mvn.scala \
      modules/mill-interceptor/src/io/eleven19/mill/interceptor/gradle/Gradle.scala \
      modules/mill-interceptor/src/io/eleven19/mill/interceptor/sbt/Sbt.scala
git commit -m "refactor: migrate Mvn, Gradle, Sbt runners from Kyo to os-lib direct style"
```

---

### Task 19: Migrate MavenSetupGenerator.scala

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/maven/MavenSetupGenerator.scala`

- [ ] **Step 1: Read and understand kyo usage**

Uses `Sync & Abort[IllegalArgumentException]` effect composition. For-comprehension over Kyo effects. Path operations via `kyo.Path`.

- [ ] **Step 2: Convert to Either-returning direct style**

Replace the for-comprehension with direct calls. File operations become `os.exists`, `os.write`, `os.makeDir.all`. Error handling becomes `Either[IllegalArgumentException, T]`. The `generate` method returns `Either[IllegalArgumentException, List[GeneratedSetupFile]]`.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/maven/MavenSetupGenerator.scala
git commit -m "refactor: migrate MavenSetupGenerator from Kyo to Either + os-lib"
```

---

### Task 20: Migrate ShimGenerator.scala

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/shim/ShimGenerator.scala`

- [ ] **Step 1: Read and understand kyo usage**

Uses `Sync` for file I/O, `kyo.Path` for filesystem operations, `Kyo.foreach` for iteration.

- [ ] **Step 2: Convert to direct style**

Replace `Sync` returns with plain returns. Replace `kyo.Path` with `os.Path`. Replace `Kyo.foreach` with `.map` or `.foreach`. Replace `path.write(content)` with `os.write(path, content)`.

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/shim/ShimGenerator.scala
git commit -m "refactor: migrate ShimGenerator from Kyo to direct style with os-lib"
```

---

### Task 21: Migrate Main.scala

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/Main.scala`

- [ ] **Step 1: Read and understand kyo usage**

Uses `KyoApp`, `Log.let(ScribeLog)`, `direct { ... }`, `Abort.run`, `Console`, `Kyo.foreach`, `Result` matching.

- [ ] **Step 2: Convert to OxApp.Simple**

Replace `KyoApp` with `OxApp.Simple`. Remove `Log.let` wrapper (Scribe is configured at startup). Replace `direct { ... }` with plain code. Replace `Abort.run[E](Cli.parse(...))` with `Cli.parse(...)` which now returns `Either`. Replace `Console.println` with `println`.

```scala
import ox.OxApp

object Main extends OxApp.Simple:
    def run(args: Vector[String])(using ox.Ox): Unit =
        Cli.parse(args.toList) match
            case Left(error) =>
                System.err.println(s"Error: ${error.getMessage}")
                System.exit(1)
            case Right(command) =>
                // dispatch command...
```

- [ ] **Step 3: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/Main.scala
git commit -m "refactor: migrate Main from KyoApp to OxApp.Simple"
```

---

### Task 22: Verify CLI module source compiles

- [ ] **Step 1: Compile**

```bash
./mill modules.mill-interceptor.compile
```

Fix any errors and commit.

---

## Phase 7: CLI Module Tests

### Task 23: Migrate CLI unit tests

**Files (17 test files):**
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/MainSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/ScribeLogSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/CliSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/MillTaskSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/maven/MavenPhaseSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/maven/MvnArgParserSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/maven/MillCommandMapperSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/maven/MavenSetupGeneratorSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimGeneratorSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimTemplateSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/BuildToolSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/gradle/GradleArgParserSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/gradle/GradleCommandMapperSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/gradle/GradleTaskSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/sbt/SbtArgParserSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/sbt/SbtCommandMapperSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/model/SharedExecutionPlanSpec.scala`
- Delete: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/ScribeLogSpec.scala` (tests the deleted ScribeLog bridge)

- [ ] **Step 1: Delete ScribeLogSpec.scala**

The ScribeLog bridge is deleted; its tests are no longer needed.

- [ ] **Step 2: Apply mechanical translations to ALL remaining test files**

Same pattern as Task 13:
1. `KyoSpecDefault` → `ZIOSpecDefault`
2. Remove `Sync.defer(...)` wrappers
3. Replace `Abort.run[E](expr).map { ... }` with direct `Either` matching
4. Replace `kyo.Path` with `os.Path`
5. Replace `Chunk` with `Seq`/`List`
6. For CliSpec: `Abort.run(Cli.parse(...))` becomes `Cli.parse(...)` directly since it returns `Either`
7. For MavenSetupGeneratorSpec/ShimGeneratorSpec: filesystem setup/teardown uses `os.*` instead of Kyo Path methods
8. For MainSpec: test the `OxApp.Simple` entry point behavior

- [ ] **Step 3: Commit**

```bash
git rm modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/ScribeLogSpec.scala
git add modules/mill-interceptor/test/
git commit -m "refactor: migrate all CLI unit tests from KyoSpecDefault to ZIOSpecDefault"
```

---

## Phase 8: Final Verification and Cleanup

### Task 24: Run full test suite

- [ ] **Step 1: Format all code**

```bash
./mill __.reformat
```

- [ ] **Step 2: Run ALL unit tests**

```bash
./mill __.test
```

Expected: All tests pass across both modules.

- [ ] **Step 3: Run integration tests**

```bash
./mill modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: All 6 integration tests pass.

- [ ] **Step 4: Run format check**

```bash
./mill __.checkFormat
```

Expected: No formatting issues.

- [ ] **Step 5: Fix any failures, re-run, commit**

---

### Task 25: Verify zero Kyo imports remain

- [ ] **Step 1: Grep for any remaining kyo imports**

```bash
grep -r "import kyo" modules/ --include="*.scala"
```

Expected: Zero results. If any remain, fix them.

- [ ] **Step 2: Grep for any remaining Kyo type usage**

```bash
grep -rn "KyoApp\|KyoSpecDefault\|Sync\.defer\|Abort\.fail\|Abort\.run\|Abort\.catching\|\.now$\|direct {" modules/ --include="*.scala"
```

Expected: Zero results (except possibly in comments or strings).

- [ ] **Step 3: Commit if any cleanup was needed**

---

### Task 26: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update the Scala filesystem guidance**

Replace the Kyo reference:

```markdown
# BEFORE:
When writing Scala in this repo, prefer Scala filesystem libraries over raw
`java.nio.file` APIs: in module application or library code, use Kyo's file
capabilities first, then `os-lib`; in `mill-build/`, prefer `os-lib`
directly.

# AFTER:
When writing Scala in this repo, prefer `os-lib` for filesystem operations
over raw `java.nio.file` APIs. In `mill-build/`, use `os-lib` directly.
Only drop to Java NIO at interop or platform boundaries.

## Direct-Style Architecture (Ox + PureLogic)

This codebase uses direct-style Scala 3:
- **Ox** for concurrency and application entry points (`OxApp`)
- **PureLogic** for typed errors in pure domain logic via context functions
- **os-lib** for filesystem operations
- **Scribe** for logging (direct calls, no effect wrapper)
- **ZIO Test** for unit and integration tests

Do NOT use: Kyo, cats-effect, ZIO effects, Futures, or monadic style.
Use plain Scala control flow. Blocking I/O is fine (virtual threads).
Use `Either[E, A]` for typed errors at I/O boundaries.
Use PureLogic `Abort[E]` for typed errors in pure domain functions.
```

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for Ox + PureLogic direct-style architecture"
```

---

### Task 27: Update release notes blog post

**Files:**
- Create: `docs/_blog/_posts/2026-03-29-direct-style-migration.md`

- [ ] **Step 1: Write the release notes**

Document the migration from Kyo to Ox + PureLogic. Cover:
- Why the migration was done (Kyo Path bug, effect boundary complexity, direct style benefits)
- What changed (dependency swap, code simplification)
- Impact (no behavioral changes, same CLI/plugin behavior, simpler code)

- [ ] **Step 2: Commit**

```bash
git add docs/_blog/_posts/2026-03-29-direct-style-migration.md
git commit -m "docs: add direct-style migration release notes"
```

---

### Task 28: Push and verify CI

- [ ] **Step 1: Push**

```bash
git push
```

- [ ] **Step 2: Wait for CI green**

All three checks must pass: Lint, Build & Test, Docs Build.

- [ ] **Step 3: Create PR**

```bash
gh pr create --title "refactor: migrate from Kyo to Ox + PureLogic direct style" --body "..."
```

---
