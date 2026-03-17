# Mill JVM Opts Warning Suppression Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Suppress the Scala/JDK `sun.misc.Unsafe` warning in normal repo-local Mill usage.

**Architecture:** Add a checked-in `.mill-jvm-opts` file so Mill starts its JVM with `--sun-misc-unsafe-memory-access=allow`, then verify the warning no longer appears in routine commands.

**Tech Stack:** Mill `1.1.3`, JDK 25, repo-local Mill config files

---

### Task 1: Confirm Mill config support

**Files:**
- Read: `.mill-jvm-version`
- Read: `mill`

**Step 1: Confirm the repo's Mill JVM pin**

Run: `sed -n '1,50p' .mill-jvm-version`
Expected: GraalVM Java 25 is pinned.

**Step 2: Confirm Mill recognizes `mill-jvm-opts`**

Inspect local Mill sources or constants to verify `mill-jvm-opts` is a known
config file name.

**Step 3: Commit the design and plan docs**

```bash
git add docs/plans/2026-03-17-mill-jvm-opts-warning-suppression-design.md docs/plans/2026-03-17-mill-jvm-opts-warning-suppression.md
git commit -m "docs: add mill jvm opts warning suppression plan"
```

### Task 2: Add the repo-local Mill JVM opts file

**Files:**
- Create: `.mill-jvm-opts`

**Step 1: Add the JVM flag**

Write:

```text
--sun-misc-unsafe-memory-access=allow
```

**Step 2: Keep the file minimal**

Do not add unrelated JVM tuning or comments unless Mill config syntax clearly
supports them and they add value.

### Task 3: Verify warning suppression

**Files:**
- Modify: none unless verification exposes a problem

**Step 1: Run a representative Mill command**

Run: `./mill --no-server show modules.mill-interceptor.publishArtifactSummary`

Expected:
- command succeeds
- the `sun.misc.Unsafe::objectFieldOffset` warning is no longer printed

**Step 2: Verify another Mill command**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.publishedPluginJar`

Expected:
- command succeeds
- warning stays suppressed

**Step 3: Commit**

```bash
git add .mill-jvm-opts
git commit -m "build: suppress mill jdk unsafe warning"
```

### Task 4: Land the work

**Files:**
- Modify: none unless follow-up is required

**Step 1: Close the issue**

Run: `bd close MI-m9j --reason "Fixed" --json`

**Step 2: Push tracker and branch**

Run:
- `bd dolt push --json`
- `git push -u origin mi-m9j-mill-jvm-opts`

**Step 3: Report results**

Include whether `.mill-jvm-opts` alone was sufficient or whether a launcher
patch would still be needed.
