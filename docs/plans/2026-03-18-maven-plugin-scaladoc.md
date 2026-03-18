# Maven Plugin Scaladoc Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add concise Scaladoc to the new Maven plugin config and lifecycle types while tracking a broader docs sweep separately.

**Architecture:** Keep the current PR focused on public type documentation only. Document the config and lifecycle model entrypoints that callers will read first, and track a broader plugin-wide docs pass as separate work.

**Tech Stack:** Scala 3, Mill, beads

---

### Task 1: Document public config types

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/EffectiveConfig.scala`

**Step 1: Add concise Scaladoc**

Document the public config case classes, overlays, and exception with short comments covering defaults and merge intent.

**Step 2: Verify formatting**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat`
Expected: PASS

### Task 2: Document lifecycle model types

**Files:**
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/LifecycleBaseline.scala`
- Modify: `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/ExecutionMode.scala`

**Step 1: Add concise Scaladoc**

Document the execution mode enum, lifecycle baseline types, and resolver entrypoint.

**Step 2: Verify formatting and tests**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.test`
Expected: PASS

### Task 3: Track the broader docs sweep

**Step 1: Create a beads task**

Run: `bd create "Broaden Maven plugin Scaladoc coverage" --description="Add a sweeping Scaladoc pass across the remaining Maven plugin public APIs after the lifecycle baseline branch lands." -t task -p 2 --json`
Expected: New issue id returned
