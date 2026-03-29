# Fixing Config Loading: Three Bugs, One Root Cause Investigation

We shipped a set of fixes that repair configuration loading in the Maven plugin. These bugs meant that `mill-interceptor.yaml` and `mill-interceptor.pkl` config files were silently ignored in every build --- the plugin fell back to hardcoded baseline defaults and nobody noticed because the defaults happened to produce passing test results.

Here is what broke, how we found it, and what we changed.

## The trigger

We set out to add a new integration test fixture that exercises PKL-format config files overriding YAML at Maven module scope. The fixture was straightforward: a multi-module reactor where the repository-level `mill-interceptor.yaml` maps `compile` to a bogus `missing.compile` target, and the `app` module's `mill-interceptor.pkl` overrides that mapping back to a real `compile` target.

The test passed on the first run. Then we asked: does this actually prove anything?

It didn't. The baseline lifecycle defaults already map `compile` to `[compile]`. If config loading were completely broken and the plugin used only baseline defaults, the test would still pass. We had written a false positive.

## Bug 1: Empty `plugin.xml` parameter declarations

Maven plugins declare their injectable parameters in `META-INF/maven/plugin.xml`. The standard `maven-plugin-plugin` generates this descriptor from `@Parameter` annotations, but this project uses a custom Mill-based generator in `Modules.scala`. That generator hardcoded every mojo with:

```xml
<parameters/>
<configuration/>
```

No parameters declared means Maven has nothing to inject. Every `@Parameter` field in `AbstractForwardingMojo` --- `mavenSession`, `mavenProject`, `repoRootDirectory`, `moduleRootDirectory` --- remained null at runtime.

The mojo's fallback chain resolved both `repoRoot` and `moduleRoot` to `File(".")`, which happened to be the correct directory in single-module fixtures (the working directory was the fixture root). Builds succeeded. Tests passed. Config was never loaded.

**Fix:** Declare all eight `AbstractForwardingMojo` parameters in the generated `plugin.xml` with proper types, default-value expressions (`${session}`, `${project}`, `${session.executionRootDirectory}`, `${project.basedir}`), and `editable=false`. The `DescribeMojo`, which extends `AbstractMojo` directly, keeps an empty parameter block.

## Bug 2: Kyo `Path` drops the leading `/`

With parameter injection working, we expected config files to be discovered. They weren't. Diagnostic logging showed:

```
repoYaml(nio)=/home/.../fixture/mill-interceptor.yaml  exists=true
kyoChild.toJava=home/.../fixture/mill-interceptor.yaml  exists=false
```

`ConfigDiscovery` constructed child paths with `Path(repoRoot, "mill-interceptor.yaml")` using Kyo's `Path` API. This produced a **relative** path --- the leading `/` was stripped. Every `path.exists` check returned false. Every config file was invisible.

**Fix:** Replace Kyo's `Path(parent, child)` with a helper that delegates to `java.nio.file.Path.resolve()`:

```scala
private def childPath(parent: Path, children: String*): Path =
    Path(children.foldLeft(parent.toJava) { (p, c) => p.resolve(c) }.toString)
```

This preserves absolute path semantics regardless of how Kyo's `Path` constructor behaves internally.

## Bug 3: Mill working directory pointed at module instead of repo root

Once config loaded correctly, multi-module builds failed because Mill ran from `moduleRoot` (`fixture/app/`) instead of `repoRoot` (`fixture/`). The `build.sc` file and `.scalafmt.conf` live at the repository root. Mill couldn't find them.

**Fix:** One-line change in `MillRunner.scala`:

```diff
- resolveWorkingDirectory(plan.request.moduleRoot, config.mill.workingDirectory),
+ resolveWorkingDirectory(plan.request.repoRoot, config.mill.workingDirectory),
```

## What the tests prove now

Before these fixes, all integration tests were false positives --- they passed because baseline defaults matched expected outcomes, not because config files were loaded.

After:

| Test | What it proves |
|------|---------------|
| **strict-failure** | Single-module: YAML config loads, `validate.scalafmtTarget` causes the expected failure |
| **multi-module-overrides** | Module-level YAML overrides repo-level YAML |
| **pkl-module-override** (new) | Module-level PKL overrides repo-level YAML; `lib` fails with `missing.compile` (repo YAML applied), `app` succeeds (PKL override applied) |
| **minimal-lifecycle** | No-config baseline works |
| **publish-lifecycle** | Publish/deploy lifecycle works |
| **placeholder-goal** | Explicit goal execution works |

The new PKL test uses **differential verification**: the `lib` module has no override and fails with the repo YAML's bogus target, while the `app` module's PKL override makes it succeed. This proves the full discovery-merge-override chain end-to-end.

## Takeaway

All three bugs coexisted silently because the fallback behavior coincidentally produced correct results. The config system had never actually worked in a real build --- and the test suite confirmed it was fine.

The lesson: when a test can pass under both "feature works" and "feature is completely broken" scenarios, the test proves nothing. Differential assertions --- where one path must fail and another must succeed --- catch this class of false positive.
