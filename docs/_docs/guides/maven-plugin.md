# Maven Plugin Guide

`mill-interceptor-maven-plugin` is a Maven core extension that intercepts
Maven lifecycle phases and forwards them to equivalent Mill targets. Instead
of maintaining parallel build definitions, you keep your real build logic in
Mill and let Maven delegate to it transparently.

## How it works

The plugin registers a lifecycle participant via `.mvn/extensions.xml`. When
Maven reads the project model it binds a forwarding Mojo to every requested
lifecycle phase. Each Mojo resolves a Mill execution plan from a built-in
baseline (optionally customized via configuration files), then executes the
plan by invoking Mill as a subprocess.

## Prerequisites

- **Maven 3.9+** (the extension mechanism requires at least Maven 3.x)
- **Mill** installed or a `mill` / `millw` launcher script at the repository
  root
- A working Mill build in the same repository

## Setup

Create `.mvn/extensions.xml` in your project root:

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0
              https://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>io.eleven19.mill-interceptor</groupId>
    <artifactId>mill-interceptor-maven-plugin</artifactId>
    <version>${mill.interceptor.version}</version>
  </extension>
</extensions>
```

Replace `${mill.interceptor.version}` with the version you want to use.

With only this file in place, the plugin activates and the conventional
baseline takes effect for all standard lifecycle phases.

## Supported goals

The plugin provides ten goals. Eight forward standard Maven lifecycle phases
to Mill, and two are operational utilities.

### Lifecycle-forwarding goals

These goals are automatically bound when Maven enters the corresponding
phase. You invoke them through normal Maven commands.

| Goal | Maven phase | Default Mill targets |
|------|-------------|---------------------|
| `clean` | clean | `clean` |
| `validate` | validate | *(empty — see [Scalafmt validation](#scalafmt-validation))* |
| `compile` | compile | `compile` |
| `test` | test | `compile test` |
| `package` | package | `compile test jar` |
| `verify` | verify | `compile test` |
| `install` | install | `compile test jar publishM2Local` |
| `deploy` | deploy | `compile test jar publish` |

`install` and `deploy` require a publish-capable Mill module such as
`PublishModule`.

### Operational goals

Invoke these directly with `mvn mill-interceptor:<goal>`.

**`inspect-plan`** — renders the resolved Mill execution plan without
running any subprocesses. Useful for verifying how configuration maps Maven
phases to Mill targets:

```bash
mvn mill-interceptor:inspect-plan
```

**`describe`** — prints a summary of available plugin goals:

```bash
mvn mill-interceptor:describe
```

## Baseline lifecycle mapping

When no configuration files are present, the plugin uses a built-in baseline
that maps each Maven phase to a sensible set of Mill targets:

```
clean    → mill clean
validate → (scalafmt hook, if enabled)
compile  → mill compile
test     → mill compile test
package  → mill compile test jar
verify   → mill compile test
install  → mill compile test jar publishM2Local
deploy   → mill compile test jar publish
```

The targets accumulate as you move through the lifecycle. Running
`mvn package` invokes Mill with `compile test jar` because Maven
executes all phases up to and including the requested phase.

## Configuration

Configuration files are optional. Add them when:

- target names differ from the baseline
- repo-level or module-level lifecycle overrides are needed
- scalafmt validation checks should be customized or disabled
- the Mill executable, working directory, or environment must be changed

### Config discovery

The plugin searches for configuration files at two scopes — repository and
module — in the following order. The first file found at each scope is used.

**Repository scope** (searched from the Maven session root):

1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module scope** (searched from the current `${project.basedir}`):

5. `mill-interceptor.yaml`
6. `mill-interceptor.pkl`
7. `.config/mill-interceptor/config.yaml`
8. `.config/mill-interceptor/config.pkl`

When both a repository-scope and a module-scope config are found, they are
merged with module-scope values taking precedence over repository-scope.

### YAML configuration reference

A complete example showing all available fields and their defaults:

```yaml
# Execution mode. "strict" (default) fails fast on missing targets.
mode: strict

# Mill CLI runtime settings
mill:
  # Path or name of the Mill executable (default: "mill")
  executable: mill
  # Override the working directory for Mill invocations
  workingDirectory: null
  # Additional environment variables passed to Mill
  environment:
    MILL_VERSION: "0.12.10"

# Validate phase configuration
validate:
  # Enable scalafmt format checking during validate (default: true)
  scalafmtEnabled: true
  # Override the scalafmt target
  # (default: mill.scalalib.scalafmt/checkFormatAll)
  scalafmtTarget: null

# Lifecycle phase overrides: phase name → list of Mill targets
lifecycle:
  compile:
    - compile
  test:
    - compile
    - test
  deploy:
    - compile
    - test
    - jar
    - publish

# Explicit goal overrides: goal name → list of Mill targets
goals:
  my-custom-goal:
    - myModule.myTarget
```

### Pkl configuration reference

The same configuration can be written in Apple's Pkl language:

```pkl
mode = "strict"

mill {
  executable = "mill"
  workingDirectory = null
  environment {
    ["MILL_VERSION"] = "0.12.10"
  }
}

validate {
  scalafmtEnabled = true
  scalafmtTarget = null
}

lifecycle {
  ["compile"] = new Listing { "compile" }
  ["test"] = new Listing { "compile"; "test" }
}

goals {
  ["my-custom-goal"] = new Listing { "myModule.myTarget" }
}
```

### Configuration fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | string | `"strict"` | Execution mode. `"strict"` fails fast when configured targets are missing or unavailable. |
| `mill.executable` | string | `"mill"` | Name or path of the Mill executable. |
| `mill.workingDirectory` | string | *null* | Override the working directory for Mill. Relative paths are resolved from the module root. |
| `mill.environment` | map | *empty* | Additional environment variables passed to the Mill subprocess. |
| `validate.scalafmtEnabled` | boolean | `true` | Whether to run a scalafmt format check during the `validate` phase. |
| `validate.scalafmtTarget` | string | `mill.scalalib.scalafmt/checkFormatAll` | The Mill target to invoke for scalafmt checking. |
| `lifecycle` | map | *(baseline)* | Override the Mill targets for any lifecycle phase. Keys are phase names, values are target lists. |
| `goals` | map | *empty* | Define custom explicit goals invoked via `mvn mill-interceptor:<goal>`. |

## Scalafmt validation

By default, the `validate` phase probes for the Mill target
`mill.scalalib.scalafmt/checkFormatAll`. If the target exists (the probe
succeeds), it is invoked to check source formatting.

The probe step means that projects without scalafmt configured will not fail —
the validate phase simply becomes a no-op.

### Disabling scalafmt

**Via configuration file:**

```yaml
validate:
  scalafmtEnabled: false
```

**Via Maven property (per invocation):**

```bash
mvn validate -Dmill.interceptor.scalafmt=false
```

The property takes precedence over the configuration file, so you can disable
the check for a single run without changing your committed config.

### Custom scalafmt target

If your project uses a different formatting target:

```yaml
validate:
  scalafmtTarget: myModule.checkFormat
```

## Multi-module projects

In a multi-module Maven project, the plugin operates on each module
independently. Configuration layering makes it straightforward to set
repo-wide defaults while overriding specific modules.

### Example: repo-level defaults with a module override

**Repository root `mill-interceptor.yaml`:**

```yaml
validate:
  scalafmtEnabled: false
lifecycle:
  compile:
    - __.compile
```

**`app/mill-interceptor.yaml`** (module-level override):

```yaml
lifecycle:
  compile:
    - app.compile
```

The `app` module uses its own compile mapping (`app.compile`) while all other
modules inherit the repo-level mapping (`__.compile`). The validate config
from the repo level applies everywhere since the module config does not
override it.

### How merging works

Module-scope configuration overlays on top of repository-scope:

- **Scalar fields** (`mode`, `mill.executable`, etc.) — module value wins if
  present.
- **Map fields** (`lifecycle`, `goals`, `mill.environment`) — keys from the
  module config override the same keys from the repo config. Keys only in the
  repo config are preserved.
- **Validate fields** — module value wins per field if present.

## Mill executable resolution

The plugin searches for a Mill executable in this order:

1. **Custom executable** — if `mill.executable` is set in configuration, that
   value is used directly.
2. **Module-local launcher** — looks for `mill` then `millw` in the module
   root directory.
3. **Repo-root launcher** — looks for `mill` then `millw` in the repository
   root directory.
4. **System PATH** — falls back to the bare `mill` command.

This means if you have a `millw` wrapper script at your repository root (a
common pattern), the plugin finds and uses it automatically.

## Property forwarding

The plugin forwards certain Maven properties to Mill:

- **`maven.repo.local`** — if set, passed to Mill as
  `-Dmaven.repo.local=<value>`. This ensures Mill publishes to the same
  local repository Maven is using (relevant for the `install` phase).

## Strict failure model

With the default `mode: strict`, the plugin fails fast when:

- A configured lifecycle target does not exist in the Mill build
- A scalafmt probe target is configured but unavailable
- Mill returns a non-zero exit code

Failures surface as Maven build errors with a descriptive message and
guidance on how to resolve the issue.

## Troubleshooting

### Inspecting the execution plan

Before debugging failures, use `inspect-plan` to see exactly what the plugin
will run:

```bash
mvn mill-interceptor:inspect-plan
```

This renders each step (probe, invoke, or fail) without executing anything.

### Common issues

**"Mill executable not found"**

Ensure `mill` or `millw` is on your PATH or present at the repository root.
Alternatively, configure the path explicitly:

```yaml
mill:
  executable: /path/to/mill
```

**"Target not found" during validate**

The scalafmt probe could not resolve the default target. Either add scalafmt
support to your Mill build or disable the check:

```yaml
validate:
  scalafmtEnabled: false
```

**Module-specific targets not resolving**

If your Mill build uses a different module structure, override the lifecycle
mapping for that module with a module-scoped config file:

```yaml
lifecycle:
  compile:
    - myModule.compile
  test:
    - myModule.compile
    - myModule.test
```

**Custom deploy arguments**

For deploy targets that need additional arguments (usernames, repository
URIs), specify them inline in the target list:

```yaml
lifecycle:
  deploy:
    - mill.javalib.MavenPublishModule/
    - --publishArtifacts
    - publishArtifacts
    - --username
    - "${env.DEPLOY_USER}"
    - --password
    - "${env.DEPLOY_PASS}"
    - --releaseUri
    - https://repo.example.com/releases
    - --snapshotUri
    - https://repo.example.com/snapshots
```

### Getting more information

Use `mvn -X` (debug mode) along with Mill Interceptor to see detailed
logging of config discovery, plan resolution, and subprocess execution.
