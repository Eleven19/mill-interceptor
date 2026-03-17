# Publish API Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace deprecated Mill `publishArtifacts` usage in the custom publish code without changing published outputs.

**Architecture:** Update the custom publish modules to use Mill's current payload-oriented publish hooks. Keep the public publish surface unchanged while migrating the Maven plugin lookup path and the assembly/native custom publishers in stages.

**Tech Stack:** Mill `1.1.3`, Scala 3, Mill publish modules, os-lib

---

### Task 1: Confirm the current deprecated touchpoints

**Files:**
- Read: `mill-build/src/build/Modules.scala`
- Read: `mill-build/src/build/PublishSupport.scala`

**Step 1: Locate the deprecated read path in `MavenPluginSupport`**

Run: `rg -n "publishArtifacts" mill-build/src/build/Modules.scala`
Expected: `publishedPluginJar` and `publishedPluginPom` call the deprecated API.

**Step 2: Locate the deprecated custom overrides in `PublishSupport`**

Run: `rg -n "override def publishArtifacts" mill-build/src/build/PublishSupport.scala`
Expected: assembly and native publish modules override the deprecated task.

**Step 3: Inspect Mill's current publish task surface**

Run project-appropriate introspection on the local Mill jars or task graph so
the implementation targets the current payload hook rather than guessing.

**Step 4: Commit the design and plan docs**

```bash
git add docs/plans/2026-03-17-publish-api-cleanup-design.md docs/plans/2026-03-17-publish-api-cleanup.md
git commit -m "docs: add publish api cleanup plan"
```

### Task 2: Migrate `MavenPluginSupport`

**Files:**
- Modify: `mill-build/src/build/Modules.scala`

**Step 1: Write the failing or diagnostic check**

Run: `./mill --no-server modules.mill-interceptor-maven-plugin.publishedPluginJar`
Expected: current task succeeds but still relies on deprecated internals.

**Step 2: Replace the deprecated jar/pom lookup**

Change `publishedPluginJar` and `publishedPluginPom` to read from Mill's current
publish payload task instead of `publishArtifacts()`.

**Step 3: Verify the Maven plugin payload lookup**

Run:
- `./mill --no-server modules.mill-interceptor-maven-plugin.publishedPluginJar`
- `./mill --no-server modules.mill-interceptor-maven-plugin.publishedPluginPom`

Expected: both tasks still resolve the expected files.

**Step 4: Commit**

```bash
git add mill-build/src/build/Modules.scala
git commit -m "build: update maven plugin publish payload lookup"
```

### Task 3: Migrate `PublishSupport`

**Files:**
- Modify: `mill-build/src/build/PublishSupport.scala`

**Step 1: Replace deprecated custom publish overrides**

Move the assembly and native publish module logic from `publishArtifacts` onto
Mill's replacement payload hook while preserving:
- current artifact metadata
- current output filenames
- current archive generation logic

**Step 2: Verify publish artifact summary remains unchanged**

Run: `./mill --no-server show modules.mill-interceptor.publishArtifactSummary`
Expected: same coordinates as before.

**Step 3: Verify the custom publish modules still resolve**

Run:
- `./mill --no-server modules.mill-interceptor.assemblyPublish.publishArtifactsDefaultPayload`
- `./mill --no-server modules.mill-interceptor.nativeLinuxAmd64Publish.publishArtifactsDefaultPayload`

Adjust the exact task names if Mill exposes a different replacement payload task
surface at implementation time.

**Step 4: Commit**

```bash
git add mill-build/src/build/PublishSupport.scala
git commit -m "build: migrate custom publish modules off deprecated api"
```

### Task 4: Run staged verification

**Files:**
- Modify: none unless verification exposes a problem

**Step 1: Verify the Maven plugin integration path**

Run:
- `./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked`

Expected: pass.

**Step 2: Verify publish metadata**

Run:
- `bash scripts/ci/test-publish-central.sh`
- `scripts/ci/test-publish-metadata.sh 1.2.3`

Expected: pass.

**Step 3: Confirm the deprecation warning is gone**

Run the relevant Mill build command and inspect output for `publishArtifacts`
deprecation warnings.

**Step 4: If warnings remain, fix them before proceeding**

Do not close the issue while deprecated publish API usage remains in the touched
build path.

### Task 5: Land the work

**Files:**
- Modify: none unless final adjustments are needed

**Step 1: Verify git state**

Run: `git status -sb`
Expected: clean working tree after commits.

**Step 2: Close the issue**

Run: `bd close MI-dyl --reason "Fixed" --json`
Expected: issue closes successfully.

**Step 3: Push tracker and git state**

Run:
- `bd dolt push --json`
- `git push -u origin mi-dyl-publish-api-cleanup`

Expected: tracker sync and branch push both succeed.

**Step 4: Open a PR**

Create a PR summarizing the publish API cleanup and the verification commands.
