# PKL Native Warning Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the PKL/Graal native-access warning from Maven plugin tests on JDK 25 with the narrowest safe fix.

**Architecture:** Reproduce the warning in the smallest Maven plugin test path, determine whether PKL/Graal has a local configuration knob, and otherwise apply a test-scoped JVM configuration so warning suppression does not leak into unrelated modules.

**Tech Stack:** Scala 3, Mill, PKL, Graal/Truffle runtime, Kyo, Mill test tasks

---

### Task 1: Reproduce and isolate the warning

**Files:**
- Inspect: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala`
- Inspect: `modules/mill-interceptor-maven-plugin/test/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoaderSpec.scala`
- Modify if needed: `modules/mill-interceptor-maven-plugin/test/src/...` to add a targeted reproducer

**Step 1: Run the smallest reproducer**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.config.ConfigLoaderSpec`
Expected: PASS with the native-access warning visible in output.

**Step 2: Tighten the reproducer if needed**

If the warning is broader than one spec, add or adjust a focused test path that triggers PKL loading directly.

**Step 3: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/test
git commit -m "test: isolate pkl native warning reproducer"
```

### Task 2: Implement the narrow suppression/fix

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/package.mill.yaml`
- Optionally modify: `mill-build/src/build/Modules.scala`
- Optionally modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala`

**Step 1: Try the narrowest safe configuration**

Prefer a Maven-plugin-test-only JVM option or environment setting over changing production evaluator behavior.

**Step 2: Run the reproducer**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test.testOnly io.eleven19.mill.interceptor.maven.plugin.config.ConfigLoaderSpec`
Expected: PASS with the warning gone.

**Step 3: Write minimal implementation**

Apply the smallest fix that removes the warning while keeping test behavior unchanged.

**Step 4: Commit**

```bash
git add modules/mill-interceptor-maven-plugin/package.mill.yaml mill-build/src/build/Modules.scala modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala
git commit -m "test: suppress pkl native access warning"
```

### Task 3: Verify the full affected surface

**Files:**
- Modify: `docs/plans/2026-03-25-pkl-native-warning-design.md`
- Modify: `docs/plans/2026-03-25-pkl-native-warning.md`

**Step 1: Run verification**

Run:
- `./mill --no-server modules.mill-interceptor-maven-plugin.test`
- `./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat`

Expected: PASS and no PKL native-access warning in the relevant test output.

**Step 2: Record the chosen fix**

Keep the docs aligned with the actual narrow fix that landed.

**Step 3: Commit**

```bash
git add docs/plans modules/mill-interceptor-maven-plugin
git commit -m "docs: record pkl warning resolution"
```

## Result

The implemented fix adds a new `PklEnabledScalaTestModule` trait in `mill-build` that appends `--enable-native-access=ALL-UNNAMED` to test fork arguments, and the Maven plugin module opts into that trait for both `test` and `itest`.
