# Kyo Filesystem Sweep Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace raw filesystem and process usage in module application and test code with Kyo-first abstractions, while keeping `mill-build/` on `os-lib`.

**Architecture:** Refactor the module code in two slices. First, move the CLI and shim generation path types and filesystem operations to Kyo-owned abstractions. Second, refactor the Maven plugin integration test helpers to use Kyo-first path and process handling instead of raw Java NIO and `scala.sys.process`. Keep Mill build code unchanged apart from any doc clarification needed for the scope boundary.

**Tech Stack:** Scala 3, Kyo, Mill declarative YAML, existing ZIO Test/Kyo test setup, Maven CLI integration tests, `os-lib` for Mill build code only, beads

---

### Task 1: Prepare An Isolated Worktree

**Files:**
- Modify: none

**Step 1: Create a dedicated worktree**

Run:

```bash
git check-ignore -q .worktrees
git worktree add .worktrees/mi-d4e-kyo-filesystem-sweep -b mi-d4e-kyo-filesystem-sweep
```

**Step 2: Move into the worktree**

Run:

```bash
cd .worktrees/mi-d4e-kyo-filesystem-sweep
git status -sb
```

Expected: clean branch `mi-d4e-kyo-filesystem-sweep`

**Step 3: Verify baseline**

Run:

```bash
./mill --no-server modules.mill-interceptor.test
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: PASS before changes. If baseline fails, stop and investigate before editing.

**Step 4: Commit if `.gitignore` needed changes**

Only if required by the worktree safety check.

### Task 2: Write Failing Tests For The CLI/Shim Slice

**Files:**
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/CliSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimGeneratorSpec.scala`

**Step 1: Add path-type expectations to CLI tests**

Update CLI parsing tests so they assert the parsed output directory uses the new Kyo-backed path model instead of Java NIO.

**Step 2: Add real shim-generation filesystem tests**

Add tests that:
- create a temporary directory through the chosen Kyo path approach
- run `ShimGenerator.generate`
- assert the expected shim files exist and contain the expected script text

**Step 3: Run the focused tests to verify failure**

Run:

```bash
./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.CliSpec
./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.shim.ShimGeneratorSpec
```

Expected: FAIL because production code still exposes Java NIO and uses the old filesystem implementation.

**Step 4: Commit**

```bash
git add modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/CliSpec.scala modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimGeneratorSpec.scala
git commit -m "test: add Kyo filesystem expectations for CLI and shims"
```

### Task 3: Implement The CLI/Shim Refactor

**Files:**
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/Cli.scala`
- Modify: `modules/mill-interceptor/src/io/eleven19/mill/interceptor/shim/ShimGenerator.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/CliSpec.scala`
- Modify: `modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimGeneratorSpec.scala`

**Step 1: Change the public shim path model**

Update:
- `ShimGenerateOptions.outputDir`
- `GeneratedShim.path`

to use the chosen Kyo path type instead of `java.nio.file.Path`.

**Step 2: Update CLI parsing minimally**

Parse `--output-dir` into the new path type and keep CLI behavior unchanged.

**Step 3: Replace raw filesystem operations in `ShimGenerator`**

Use Kyo-first path and filesystem operations for:
- directory creation
- file path resolution
- file writes
- any executable-bit handling that can stay inside the Kyo-supported model

If a narrow POSIX-permission interop seam still requires Java APIs, keep that seam local and explicit.

**Step 4: Run the focused tests to verify green**

Run:

```bash
./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.CliSpec
./mill --no-server modules.mill-interceptor.test.testOnly io.eleven19.mill.interceptor.shim.ShimGeneratorSpec
```

Expected: PASS

**Step 5: Run the full app-module test suite**

Run:

```bash
./mill --no-server modules.mill-interceptor.test
```

Expected: PASS

**Step 6: Commit**

```bash
git add modules/mill-interceptor/src/io/eleven19/mill/interceptor/Cli.scala modules/mill-interceptor/src/io/eleven19/mill/interceptor/shim/ShimGenerator.scala modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/CliSpec.scala modules/mill-interceptor/test/src/io/eleven19/mill/interceptor/shim/ShimGeneratorSpec.scala
git commit -m "refactor: use Kyo filesystem abstractions in CLI and shims"
```

### Task 4: Write Failing Tests For The Maven Plugin Integration Slice

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Refactor the integration spec expectations first**

Restructure the test helper code so the spec expects Kyo-owned path and process handling.

**Step 2: Add assertions around command execution output if needed**

Keep the behavior-level assertion the same, but make the helper boundary explicit enough that the old implementation no longer compiles or passes unchanged.

**Step 3: Run the plugin integration suite to verify failure**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: FAIL because the helper implementation still uses raw Java NIO and `scala.sys.process`.

**Step 4: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala
git commit -m "test: prepare Maven plugin itest for Kyo filesystem helpers"
```

### Task 5: Implement The Maven Plugin Integration Refactor

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

**Step 1: Replace raw temp-dir and copy helpers**

Move temporary-directory setup, fixture copying, PATH traversal, and cleanup to Kyo-first path utilities.

**Step 2: Replace `scala.sys.process`**

Use Kyo process abstractions for Maven and helper command execution, capturing output in a test-friendly form.

**Step 3: Keep interop seams explicit**

If Maven test setup or classloader resource conversion still requires Java path interop, keep conversion local and narrow.

**Step 4: Run the integration suite to verify green**

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: PASS

**Step 5: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala
git commit -m "refactor: use Kyo filesystem and process APIs in Maven plugin itest"
```

### Task 6: Clarify The Guidance Boundary

**Files:**
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `.github/copilot-instructions.md`
- Modify: `docs/contributing/README.md`

**Step 1: Update wording to distinguish module code from Mill build code**

Clarify that:
- application and library code should prefer Kyo first, then `os-lib`
- `mill-build/` code should prefer `os-lib`

**Step 2: Run a quick diff review**

Run:

```bash
git diff -- AGENTS.md CLAUDE.md .github/copilot-instructions.md docs/contributing/README.md
```

Expected: wording-only clarification

**Step 3: Commit**

```bash
git add AGENTS.md CLAUDE.md .github/copilot-instructions.md docs/contributing/README.md
git commit -m "docs: clarify filesystem guidance by code layer"
```

### Task 7: Final Verification And Landing

**Files:**
- Modify: none

**Step 1: Run the relevant quality gates**

Run:

```bash
./mill --no-server __.checkFormat
./mill --no-server modules.mill-interceptor.test
./mill --no-server modules.mill-interceptor-maven-plugin.test
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

Expected: PASS

**Step 2: Review changed filesystem call sites**

Run:

```bash
rg -n "java\\.nio\\.file|Paths\\.|Files\\.|scala\\.sys\\.process" modules -g '*.scala'
```

Expected: no remaining hits in module application/test code except narrow, justified interop boundaries

**Step 3: Update beads**

Run:

```bash
bd close MI-d4e --reason "Replaced raw filesystem and process usage in module code with Kyo-first abstractions and clarified the Mill-build os-lib boundary." --json
```

**Step 4: Push everything**

Run:

```bash
git pull --rebase
bd dolt push --json
git push --set-upstream origin mi-d4e-kyo-filesystem-sweep
git status -sb
```

Expected: branch pushed and clean
