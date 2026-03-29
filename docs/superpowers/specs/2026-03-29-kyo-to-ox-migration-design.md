# Kyo to Ox + PureLogic Migration

**Date**: 2026-03-29
**Status**: Approved
**Scope**: Full codebase migration across both modules in a single branch

## Goal

Replace Kyo effect system with direct-style Scala 3 using Ox (concurrency, application entry point) and PureLogic (typed errors, reader/state for pure domain logic). The codebase moves from monadic effect composition to plain Scala with virtual threads and context functions.

## Motivation

- Kyo's `Path(parent, child)` constructor silently drops the leading `/` (caused a production bug — PR #29)
- Kyo's `Sync.Unsafe.evalOrThrow` makes the effect boundary awkward at Maven plugin integration points
- Direct-style code is simpler to read, debug, and profile — stack traces show domain logic, not effect runtime internals
- PureLogic's context-function approach gives typed errors without monadic overhead (7-40x faster, 10-50x less allocation than monadic alternatives)
- Ox provides structured concurrency for the CLI module without an effect system

## Modules

Two modules, migrated sequentially:

1. **`mill-interceptor-maven-plugin`** — Maven plugin, runs inside Maven JVM. Smaller, no concurrency needs. Migrated first.
2. **`mill-interceptor`** — CLI tool, compiled to GraalVM native-image. Uses `KyoApp`, `Process`, `Log`. Migrated second.

Each module is verified (compile, tests, CI green) before proceeding to the next.

## Current Kyo Footprint

- **49 files** use Kyo (14 source, 32 test, 3 config)
- **Effects used**: `Sync`, `Abort`, `Log`, `Process`, `Path`
- **Test framework**: ZIO Test via `KyoSpecDefault` bridge
- **Entry point**: `KyoApp` (CLI module)

## Translation Rules

### Types and effects

| Kyo | Direct Style |
|---|---|
| `T < Sync` | `T` (plain return) |
| `T < Abort[E]` | `Either[E, T]` or PureLogic `Abort[E]` context |
| `T < (Abort[E] & Sync)` | `Either[E, T]` (I/O boundary) or PureLogic `Abort[E]` (domain) |
| `Abort.fail(e)` | `Left(e)` or `purelogic.fail(e)` |
| `Abort.run[E](expr)` | `expr` (already `Either`) or `purelogic.Abort { expr }` |
| `Abort.catching[E] { ... }` | `try/catch` or `purelogic.Abort.attempt { ... }` |
| `Sync.defer { ... }` | `{ ... }` (direct call) |
| `Kyo.foreach(seq)(f)` | `seq.map(f)` |
| `Kyo.foldLeft(seq)(init)(f)` | `seq.foldLeft(init)(f)` |
| `Kyo.unit` | `()` |
| `direct { ... }` | Remove wrapper |
| `.now` | Remove |

### Path operations

| Kyo | os-lib |
|---|---|
| `kyo.Path` | `os.Path` |
| `Path(string)` | `os.Path(java.nio.file.Paths.get(string))` |
| `Path(parent, child)` | `parent / child` |
| `path.exists` | `os.exists(path)` |
| `path.read` | `os.read(path)` |
| `path.write(content)` | `os.write(path, content)` |
| `path.mkDir` | `os.makeDir.all(path)` |
| `path.toJava` | `path.toNIO` |
| `path.removeAll` | `os.remove.all(path)` |

### Process execution

| Kyo | os-lib |
|---|---|
| `Process.Command(args*).cwd(dir).waitFor` | `os.proc(args).call(cwd = dir)` |
| `Process.Input.Inherit` | `os.Inherit` |
| `Process.Output.Inherit` | `os.Inherit` |

### Logging

| Kyo | Direct |
|---|---|
| `Log.info(msg)` | `scribe.info(msg)` (direct Scribe call) |
| `Log.let(impl)(body)` | Remove wrapper, configure Scribe at startup |
| `Log.Unsafe` trait | Remove (no custom bridge needed) |

### Application entry point

| Kyo | Ox |
|---|---|
| `KyoApp` | `OxApp.Simple` |

### Test framework

| Kyo | Direct |
|---|---|
| `KyoSpecDefault` | `ZIOSpecDefault` |
| `Sync.defer(assertTrue(...))` | `assertTrue(...)` |
| `Abort.run[E](expr).map { ... }` | Direct `Either` match |

## Layer Responsibilities

| Layer | Library | When to use |
|---|---|---|
| Pure domain logic | PureLogic | Config merging, plan resolution, lifecycle mapping, validation |
| I/O boundary | Direct Scala + os-lib | File reads, subprocess execution, Maven mojo bridge |
| Error propagation at I/O boundary | `Either` + pattern match | ConfigLoader, PklConfigEvaluator |
| Concurrency (CLI) | Ox | `OxApp.Simple` entry point, `par()` if needed |

### PureLogic usage guidelines

- Use `purelogic.Abort[E]` for typed errors in pure domain functions (no I/O)
- Use `Abort { expr }` at the boundary to get `Either[E, A]`
- Functions that do I/O return `Either[E, A]` directly (not PureLogic context)
- PureLogic `Reader`, `Writer`, `State` available for domain logic that needs them but not required — adopt incrementally where they add clarity

## Dependencies

### Remove

- `io.getkyo::kyo-core::1.0-RC1`
- `io.getkyo::kyo-direct::1.0-RC1`
- `io.getkyo::kyo-prelude::1.0-RC1`
- `io.getkyo::kyo-zio-test::1.0-RC1`

### Add

- `com.softwaremill.ox::core::1.0.4` (CLI module — `OxApp`, `either:` blocks)
- `com.github.ghostdogpr::purelogic::0.2.0` (both modules — typed errors for domain logic)

### Keep

- `dev.zio::zio-test` + `dev.zio::zio-test-sbt`
- `com.lihaoyi::os-lib`
- `com.outr::scribe`
- All other existing dependencies

## Migration Order

### Phase 1: Shared model types

Swap `kyo.Path` to `os.Path` in model types that cross module boundaries:
- `MillExecutionPlan`
- `ExecutionRequest`
- `ModuleRef`
- Any other shared types using `kyo.Path`

### Phase 2: Maven plugin module — source files (bottom-up)

1. **Config layer**: `ConfigDiscovery`, `ConfigLoader`, `PklConfigEvaluator`, `EffectiveConfig` (overlays, merge logic)
2. **Model layer**: `LifecycleBaseline`
3. **Resolve layer**: `ExecutionPlanResolver`
4. **Exec layer**: `MillRunner`
5. **Mojo layer**: `AbstractForwardingMojo`, all concrete mojos
6. **Build**: `MojoDescriptorMeta` (uses `kyo.Path` in method signatures)

### Phase 3: Maven plugin module — tests

- Replace `KyoSpecDefault` with `ZIOSpecDefault` in all 13+ test files
- Remove Kyo effect wrappers from assertions
- Verify: compile + unit tests + integration tests green

### Phase 4: Push and verify CI

- Push plugin module migration
- CI must be green before proceeding

### Phase 5: CLI module — source files (bottom-up)

1. **Logging**: Remove `ScribeLog` (Kyo `Log.Unsafe` bridge), use Scribe directly
2. **Build tool runners**: `Mvn`, `Gradle`, `Sbt` — replace `Process`/`direct{}` with `os.proc`
3. **Generators**: `MavenSetupGenerator`, `ShimGenerator` — replace `Sync`/`Abort`/`Path`
4. **CLI**: `Cli.scala` — replace `Abort[IllegalArgumentException]` with `Either` or PureLogic `Abort`
5. **Entry point**: `Main.scala` — replace `KyoApp` with `OxApp.Simple`

### Phase 6: CLI module — tests

- Same pattern: `KyoSpecDefault` → `ZIOSpecDefault`, remove effect wrappers
- Verify: compile + all tests green

### Phase 7: Dependency cleanup and final verification

- Remove Kyo dependencies from both `package.mill.yaml` files
- Add Ox and PureLogic dependencies
- Full test suite green
- Push, CI green

### Phase 8: Documentation

- Update `CLAUDE.md` to reference Ox/PureLogic instead of Kyo
- Update release notes blog post
- Update any docs referencing Kyo patterns

## CLAUDE.md Guidance for Migration Work

Add to CLAUDE.md during migration:

```
## Direct-Style Migration (Ox + PureLogic)

This codebase uses direct-style Scala 3 with:
- **Ox** for concurrency and application entry points
- **PureLogic** for typed errors and pure domain logic via context functions
- **os-lib** for filesystem operations
- **Scribe** for logging (direct calls, no effect wrapper)

Do NOT use: Kyo, cats-effect, ZIO effects, Futures, or monadic style.
Use plain Scala control flow. Blocking I/O is fine (virtual threads).
Use `Either[E, A]` for typed errors at I/O boundaries.
Use PureLogic `Abort[E]` for typed errors in pure domain functions.
```

## Success Criteria

- Zero Kyo imports remaining in the codebase
- All unit tests pass (60+ tests)
- All integration tests pass (6 tests)
- CI green (Lint, Build & Test, Docs Build)
- No behavioral changes — same config loading, same lifecycle mapping, same CLI behavior
