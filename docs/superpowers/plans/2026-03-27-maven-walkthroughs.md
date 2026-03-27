# Maven Plugin Walkthroughs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five self-contained walkthrough pages for common Maven plugin scenarios to the documentation site.

**Architecture:** Each walkthrough is an independent Markdown page in `docs/_docs/guides/maven/`. The sidebar is updated once to include all five pages. All pages are verified by running the Scaladoc build.

**Tech Stack:** Markdown prose pages, Scaladoc static site generation, highlight.js (xml/yaml/bash supported; pkl renders as plain text per MI-8jq)

**Spec:** `docs/superpowers/specs/2026-03-27-maven-walkthroughs-design.md`

---

### Task 1: Create directory and sidebar structure

**Files:**
- Create: `docs/_docs/guides/maven/` (directory)
- Modify: `docs/sidebar.yml`

- [ ] **Step 1: Create the maven walkthroughs directory**

```bash
mkdir -p docs/_docs/guides/maven
```

- [ ] **Step 2: Update sidebar.yml**

Replace the full contents of `docs/sidebar.yml` with:

```yaml
index: index.md
subsection:
  - title: Getting Started
    page: getting-started.md
  - title: Usage
    page: usage.md
  - title: Guides
    subsection:
      - title: Maven Plugin
        page: guides/maven-plugin.md
      - title: Maven Walkthroughs
        subsection:
          - title: Getting Started
            page: guides/maven/getting-started.md
          - title: Package and Deploy
            page: guides/maven/package-and-deploy.md
          - title: Multi-Module Projects
            page: guides/maven/multi-module.md
          - title: Debugging with inspect-plan
            page: guides/maven/debugging-with-inspect.md
          - title: Pkl Configuration
            page: guides/maven/pkl-configuration.md
      - title: Release Artifacts
        page: guides/release-artifacts.md
```

- [ ] **Step 3: Commit**

```bash
git add docs/sidebar.yml
git commit -m "docs: add sidebar structure for Maven walkthrough pages"
```

---

### Task 2: Write Getting Started walkthrough

**Files:**
- Create: `docs/_docs/guides/maven/getting-started.md`

- [ ] **Step 1: Write the page**

Create `docs/_docs/guides/maven/getting-started.md` with this content:

````markdown
# Getting Started with Maven Interception

This walkthrough takes you from an existing Maven and Mill project to a
working intercepted build where Maven lifecycle commands delegate to Mill.

## What you'll learn

- How to register the plugin as a Maven core extension
- What happens when you run a Maven command with interception enabled
- How the baseline maps Maven phases to Mill targets
- How to verify the plugin is active

## Prerequisites

- An existing project with both a `pom.xml` and a working Mill build
- Maven 3.9 or later
- Mill installed or a `mill`/`millw` launcher at the repository root

## Step 1: Register the extension

Create the file `.mvn/extensions.xml` in your project root:

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0
              https://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>io.eleven19.mill-interceptor</groupId>
    <artifactId>mill-interceptor-maven-plugin</artifactId>
    <version>0.4.0</version>
  </extension>
</extensions>
```

This is the only file you need. No changes to `pom.xml` are required.

## Step 2: Run a Maven command

Try compiling through Maven:

```bash
mvn compile
```

Behind the scenes, the plugin intercepts the `compile` phase and runs
`mill compile` instead. You should see Mill's output in your terminal
alongside Maven's lifecycle logging.

## Step 3: Understand the baseline

With no configuration files present, the plugin uses a built-in baseline
mapping:

| Maven command | Mill targets invoked |
|---------------|---------------------|
| `mvn clean` | `mill clean` |
| `mvn compile` | `mill compile` |
| `mvn test` | `mill compile test` |
| `mvn package` | `mill compile test jar` |
| `mvn verify` | `mill compile test` |
| `mvn install` | `mill compile test jar publishM2Local` |
| `mvn deploy` | `mill compile test jar publish` |

The `validate` phase is special — it runs an optional scalafmt format check
if your Mill build includes scalafmt support. If not, it silently passes.

Maven executes phases sequentially, so `mvn package` also runs validate,
compile, and test before package.

## Step 4: Check available goals

Run the `describe` goal to confirm the plugin is active and see all
available goals:

```bash
mvn mill-interceptor:describe
```

You can also preview what Mill commands will run for any phase without
executing them:

```bash
mvn mill-interceptor:inspect-plan
```

This dry-run mode is useful for verifying your setup before running a real
build.

## Next steps

- [Customizing Package and Deploy](package-and-deploy.md) — override which
  Mill targets run for specific phases
- [Multi-Module Projects](multi-module.md) — configure interception across
  a Maven reactor build
- [Debugging with inspect-plan](debugging-with-inspect.md) — diagnose
  issues when things go wrong
- [Pkl Configuration](pkl-configuration.md) — use Pkl instead of YAML for
  configuration
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
````

- [ ] **Step 2: Verify the build**

```bash
./mill modules.mill-interceptor.docJar
```

Expected: SUCCESS, and `out/modules/mill-interceptor/scalaDocGenerated.dest/javadoc/docs/guides/maven/getting-started.html` exists.

- [ ] **Step 3: Commit**

```bash
git add docs/_docs/guides/maven/getting-started.md
git commit -m "docs: add Getting Started Maven walkthrough"
```

---

### Task 3: Write Package and Deploy walkthrough

**Files:**
- Create: `docs/_docs/guides/maven/package-and-deploy.md`

- [ ] **Step 1: Write the page**

Create `docs/_docs/guides/maven/package-and-deploy.md` with this content:

````markdown
# Customizing Package and Deploy

This walkthrough shows how to override the Mill targets that run during
Maven's `package` and `deploy` phases — for example, producing a fat JAR
or publishing to a private registry with credentials.

## What you'll learn

- How to create a configuration file that overrides lifecycle phases
- How to pass arguments to Mill targets in the config
- How to verify your overrides with `inspect-plan`

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))
- A Mill build that exposes the targets you want to map

## Overriding the package phase

By default, `mvn package` runs `mill compile test jar`. If your Mill build
uses an assembly target for fat JARs, create a config file at the
repository root.

**`mill-interceptor.yaml`:**

```yaml
lifecycle:
  package:
    - compile
    - test
    - assembly
```

Now `mvn package` runs `mill compile test assembly` instead of the baseline.

Only the phases you list are overridden — all other phases keep their
baseline mappings.

## Overriding the deploy phase

The `deploy` phase often requires passing credentials and repository URIs
to Mill. Specify these as additional entries in the target list:

**`mill-interceptor.yaml`:**

```yaml
lifecycle:
  package:
    - compile
    - test
    - assembly
  deploy:
    - compile
    - test
    - assembly
    - publish
    - --username
    - ci-deployer
    - --password
    - "${env.DEPLOY_TOKEN}"
    - --releaseUri
    - https://repo.example.com/releases
    - --snapshotUri
    - https://repo.example.com/snapshots
```

Each entry in the list becomes a separate argument to the Mill invocation.

## Verifying with inspect-plan

Before running the actual build, preview the resolved plan:

```bash
mvn mill-interceptor:inspect-plan
```

The output shows the exact Mill command that will be executed for each
phase, including all arguments. Check that your overrides appear correctly.

## Disabling scalafmt for deploy builds

CI deploy pipelines often don't need format checks. Disable scalafmt in the
same config:

```yaml
validate:
  scalafmtEnabled: false

lifecycle:
  package:
    - compile
    - test
    - assembly
  deploy:
    - compile
    - test
    - assembly
    - publish
    - --username
    - ci-deployer
    - --password
    - "${env.DEPLOY_TOKEN}"
    - --releaseUri
    - https://repo.example.com/releases
    - --snapshotUri
    - https://repo.example.com/snapshots
```

## Tips

- You can override any phase — `clean`, `validate`, `compile`, `test`,
  `package`, `verify`, `install`, and `deploy` are all configurable.
- Setting a phase to an empty list makes it a no-op:

  ```yaml
  lifecycle:
    verify: []
  ```

- The config file is checked into source control, so the entire team gets
  the same behavior.

## Next steps

- [Multi-Module Projects](multi-module.md) — different overrides per module
- [Debugging with inspect-plan](debugging-with-inspect.md) — troubleshoot
  configuration issues
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
````

- [ ] **Step 2: Commit**

```bash
git add docs/_docs/guides/maven/package-and-deploy.md
git commit -m "docs: add Package and Deploy Maven walkthrough"
```

---

### Task 4: Write Multi-Module walkthrough

**Files:**
- Create: `docs/_docs/guides/maven/multi-module.md`

- [ ] **Step 1: Write the page**

Create `docs/_docs/guides/maven/multi-module.md` with this content:

````markdown
# Multi-Module Projects

This walkthrough shows how to configure the Maven plugin across a
multi-module reactor build where different modules need different Mill
target mappings.

## What you'll learn

- How to set shared defaults at the repository level
- How to override specific phases per module
- How config merging works between repo and module scopes
- How to verify each module's plan independently

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))
- A Maven reactor (parent POM with modules) and corresponding Mill build

## Example project structure

Consider a project with three modules:

```
my-project/
  pom.xml                  (parent POM, packaging=pom)
  .mvn/extensions.xml      (plugin registration)
  mill-interceptor.yaml    (repo-level config)
  build.mill               (Mill build)
  api/
    pom.xml
  core/
    pom.xml
  app/
    pom.xml
    mill-interceptor.yaml  (module-level override)
```

## Step 1: Set repo-level defaults

Create `mill-interceptor.yaml` at the repository root with defaults that
apply to all modules:

```yaml
validate:
  scalafmtEnabled: false

lifecycle:
  compile:
    - __.compile
  test:
    - __.compile
    - __.test
```

The `__.` prefix is Mill's syntax for targeting all modules. These defaults
apply to `api/`, `core/`, and `app/` unless overridden.

## Step 2: Add a module-level override

The `app` module needs a custom compile mapping that targets only its own
sources. Create `app/mill-interceptor.yaml`:

```yaml
lifecycle:
  compile:
    - app.compile
  test:
    - app.compile
    - app.test
```

## Step 3: Understand config merging

When both repo-level and module-level configs exist, they merge with these
rules:

| Field type | Behavior |
|-----------|----------|
| Scalar (`mode`, `mill.executable`) | Module value wins if present |
| Map (`lifecycle`, `goals`, `mill.environment`) | Module keys override matching repo keys; repo-only keys are preserved |
| Validate fields (`scalafmtEnabled`, `scalafmtTarget`) | Module value wins per field if present |

For the `app` module, the effective config is:

```yaml
# From repo-level (not overridden by module):
validate:
  scalafmtEnabled: false

# From module-level (overrides repo-level):
lifecycle:
  compile:
    - app.compile
  test:
    - app.compile
    - app.test
```

For `api` and `core`, the full repo-level config applies since they have no
module-level config files.

## Step 4: Verify each module's plan

Run `inspect-plan` from the reactor root to see all modules:

```bash
mvn mill-interceptor:inspect-plan
```

The output shows the resolved plan per module. Check that:

- `api` and `core` use `__.compile` / `__.test`
- `app` uses `app.compile` / `app.test`
- All modules have scalafmt disabled

## Config discovery per module

The plugin searches for config files at each scope independently. For each
module, the search is:

**Repository scope** (from Maven session root):
1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module scope** (from `${project.basedir}`):
5. `mill-interceptor.yaml`
6. `mill-interceptor.pkl`
7. `.config/mill-interceptor/config.yaml`
8. `.config/mill-interceptor/config.pkl`

The first file found at each scope wins. Both YAML and Pkl are supported —
you can mix formats between scopes if needed.

## Tips

- The parent POM module (with `packaging=pom`) also participates in
  interception. If it doesn't need a Mill build, set its lifecycle to
  empty mappings or exclude it.
- Use the `.config/mill-interceptor/` path if you prefer to keep config
  files out of the module root directory.
- Module-level configs only need to contain the fields you want to
  override. Everything else inherits from the repo level.

## Next steps

- [Customizing Package and Deploy](package-and-deploy.md) — override
  package and deploy phases with arguments
- [Debugging with inspect-plan](debugging-with-inspect.md) — troubleshoot
  per-module configuration issues
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
````

- [ ] **Step 2: Commit**

```bash
git add docs/_docs/guides/maven/multi-module.md
git commit -m "docs: add Multi-Module Projects Maven walkthrough"
```

---

### Task 5: Write Debugging with inspect-plan walkthrough

**Files:**
- Create: `docs/_docs/guides/maven/debugging-with-inspect.md`

- [ ] **Step 1: Write the page**

Create `docs/_docs/guides/maven/debugging-with-inspect.md` with this content:

````markdown
# Debugging with inspect-plan

This walkthrough shows how to use the `inspect-plan` goal and Maven's
debug logging to diagnose issues when the Maven plugin doesn't behave as
expected.

## What you'll learn

- How to use `inspect-plan` to preview the resolved Mill execution plan
- How to read the plan output — probe, invoke, and fail steps
- Common failure patterns and how to fix them
- How to get verbose logging with `mvn -X`

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))

## Running inspect-plan

The `inspect-plan` goal renders the full execution plan without invoking
any Mill subprocesses:

```bash
mvn mill-interceptor:inspect-plan
```

This is a dry-run mode — it resolves configuration, computes the target
list for each phase, and prints the result. No Mill commands are executed.

## Reading the output

The plan output contains three types of steps:

**Invoke steps** — Mill commands that will be executed:

```
[INFO] Step: invoke mill compile test
[INFO]   Working directory: /path/to/project
```

**Probe steps** — pre-flight checks that verify a target exists before
invoking it:

```
[INFO] Step: probe mill resolve mill.scalalib.scalafmt/checkFormatAll
[INFO]   Working directory: /path/to/project
```

Probes run `mill resolve <target>` to check if the target is available.
This is used for optional features like scalafmt validation.

**Fail steps** — steps that will cause the build to fail with a message:

```
[INFO] Step: fail "No lifecycle mapping found for phase 'site'"
[INFO]   Guidance: Check mill-interceptor.yaml or remove the unsupported phase
```

Fail steps appear when a phase has no mapping and strict mode is active.

## Common failure patterns

### Missing Mill target

**Symptom:** Build fails with a message like "target not found" or a
non-zero Mill exit code.

**Diagnosis:** Run `inspect-plan` to see what target is being invoked, then
verify it exists in your Mill build:

```bash
mvn mill-interceptor:inspect-plan
mill resolve compile    # check if the target exists
```

**Fix:** Either correct the target name in your `mill-interceptor.yaml` or
add the missing target to your Mill build.

### Scalafmt probe failure

**Symptom:** `mvn validate` fails looking for a scalafmt target.

**Diagnosis:** The plugin probes for
`mill.scalalib.scalafmt/checkFormatAll` by default. If your Mill build
doesn't include scalafmt, the probe fails.

**Fix:** Disable the scalafmt check:

```yaml
validate:
  scalafmtEnabled: false
```

Or via a one-off property:

```bash
mvn validate -Dmill.interceptor.scalafmt=false
```

### Wrong Mill executable

**Symptom:** "mill: command not found" or the wrong version of Mill is
invoked.

**Diagnosis:** The plugin searches for Mill executables in this order:
1. Custom `mill.executable` from config
2. `mill` or `millw` in the module root
3. `mill` or `millw` in the repo root
4. `mill` on the system PATH

**Fix:** Set the executable explicitly:

```yaml
mill:
  executable: ./millw
```

### Config file not being picked up

**Symptom:** Your overrides don't take effect.

**Diagnosis:** Enable debug logging to see config discovery:

```bash
mvn mill-interceptor:inspect-plan -X
```

Look for log lines about config discovery — they show which paths were
searched and which files were loaded.

**Fix:** Verify the config file is in one of the expected locations:
- `mill-interceptor.yaml` (repo or module root)
- `mill-interceptor.pkl` (repo or module root)
- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

### Multi-module config confusion

**Symptom:** One module gets the right config but another doesn't.

**Diagnosis:** Run `inspect-plan` from the reactor root to see per-module
plans. Check that module-level config files are in the correct
`${project.basedir}` directory, not a subdirectory.

**Fix:** See [Multi-Module Projects](multi-module.md) for config merging
rules.

## Verbose logging with mvn -X

When `inspect-plan` alone isn't enough, enable Maven's debug output:

```bash
mvn compile -X
```

This adds detailed logging for:
- Config file discovery (which paths were searched)
- Config loading and merging (which values came from which file)
- Execution plan resolution (how phases mapped to targets)
- Mill subprocess execution (exact commands, working directory, environment)

Filter the output by looking for lines from the
`io.eleven19.mill.interceptor` package.

## Next steps

- [Getting Started](getting-started.md) — initial setup
- [Customizing Package and Deploy](package-and-deploy.md) — override
  specific phases
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
````

- [ ] **Step 2: Commit**

```bash
git add docs/_docs/guides/maven/debugging-with-inspect.md
git commit -m "docs: add Debugging with inspect-plan Maven walkthrough"
```

---

### Task 6: Write Pkl Configuration walkthrough

**Files:**
- Create: `docs/_docs/guides/maven/pkl-configuration.md`

- [ ] **Step 1: Write the page**

Create `docs/_docs/guides/maven/pkl-configuration.md` with this content:

````markdown
# Using Pkl Configuration

This walkthrough shows how to configure the Maven plugin using Apple's Pkl
language instead of YAML. Pkl offers type checking, expressions, and
IDE support through the Pkl IntelliJ and VS Code plugins.

## What you'll learn

- How to write a Pkl configuration file for the Maven plugin
- Config discovery behavior with Pkl files
- How to mix Pkl and YAML across repo and module scopes

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))
- Basic familiarity with Pkl syntax (see
  [pkl-lang.org](https://pkl-lang.org))

## Writing a Pkl config

Create `mill-interceptor.pkl` at the repository root:

```pkl
mode = "strict"

mill {
  executable = "mill"
}

validate {
  scalafmtEnabled = true
}

lifecycle {
  ["compile"] = new Listing { "compile" }
  ["test"] = new Listing { "compile"; "test" }
  ["package"] = new Listing { "compile"; "test"; "assembly" }
}
```

This is equivalent to the following YAML:

```yaml
mode: strict

mill:
  executable: mill

validate:
  scalafmtEnabled: true

lifecycle:
  compile:
    - compile
  test:
    - compile
    - test
  package:
    - compile
    - test
    - assembly
```

## Config discovery with Pkl

The plugin discovers Pkl files in the same locations as YAML, with YAML
checked first at each scope:

**Repository scope:**
1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module scope:**
5. `mill-interceptor.yaml`
6. `mill-interceptor.pkl`
7. `.config/mill-interceptor/config.yaml`
8. `.config/mill-interceptor/config.pkl`

The first file found at each scope wins. If a `mill-interceptor.yaml`
exists, the `.pkl` file at the same scope is ignored.

## Mixing formats

You can use Pkl at one scope and YAML at another. For example:

- **Repo level:** `mill-interceptor.pkl` with shared defaults
- **Module level:** `app/mill-interceptor.yaml` with module overrides

The plugin loads each file in its own format and merges the results using
the same rules regardless of format. This is useful when teams adopt Pkl
incrementally.

## Full Pkl example with all fields

A complete configuration file showing every available field:

```pkl
// Execution mode: "strict" fails fast on missing targets
mode = "strict"

// Mill CLI runtime settings
mill {
  executable = "mill"
  workingDirectory = null

  environment {
    ["MILL_VERSION"] = "0.12.10"
    ["JAVA_OPTS"] = "-Xmx2g"
  }
}

// Validate phase configuration
validate {
  scalafmtEnabled = true
  scalafmtTarget = "mill.scalalib.scalafmt/checkFormatAll"
}

// Lifecycle phase overrides
lifecycle {
  ["clean"] = new Listing { "clean" }
  ["compile"] = new Listing { "compile" }
  ["test"] = new Listing { "compile"; "test" }
  ["package"] = new Listing { "compile"; "test"; "jar" }
  ["install"] = new Listing { "compile"; "test"; "jar"; "publishM2Local" }
  ["deploy"] = new Listing {
    "compile"
    "test"
    "jar"
    "publish"
    "--username"
    "ci-deployer"
    "--releaseUri"
    "https://repo.example.com/releases"
  }
}

// Custom explicit goals
goals {
  ["check"] = new Listing { "__.compile"; "__.test" }
}
```

## Using the .config directory

If you prefer keeping config files out of the project root, place them
under `.config/`:

```
my-project/
  .config/
    mill-interceptor/
      config.pkl         (repo-level)
  app/
    .config/
      mill-interceptor/
        config.pkl       (module-level for app)
```

The plugin discovers these paths automatically.

## Verifying Pkl config

After writing your Pkl config, use `inspect-plan` to verify it was loaded
correctly:

```bash
mvn mill-interceptor:inspect-plan
```

If the Pkl file has syntax errors, the plugin reports a
`ConfigLoadException` with the file path and error detail.

## Next steps

- [Customizing Package and Deploy](package-and-deploy.md) — practical
  example with YAML (same patterns apply to Pkl)
- [Multi-Module Projects](multi-module.md) — layered config across modules
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
````

- [ ] **Step 2: Commit**

```bash
git add docs/_docs/guides/maven/pkl-configuration.md
git commit -m "docs: add Pkl Configuration Maven walkthrough"
```

---

### Task 7: Build verification and final commit

**Files:**
- All files from Tasks 1-6

- [ ] **Step 1: Run full docs build**

```bash
./mill modules.mill-interceptor.docJar
```

Expected: SUCCESS

- [ ] **Step 2: Verify all HTML pages were generated**

```bash
ls out/modules/mill-interceptor/scalaDocGenerated.dest/javadoc/docs/guides/maven/
```

Expected output:
```
debugging-with-inspect.html
getting-started.html
multi-module.html
package-and-deploy.html
pkl-configuration.html
```

- [ ] **Step 3: Push the branch**

```bash
git push
```
