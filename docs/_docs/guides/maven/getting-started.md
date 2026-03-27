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
    <version>${mill.interceptor.version}</version>
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

The `validate` phase is special — it probes for a scalafmt format-check
target. If your Mill build includes scalafmt, the check runs automatically.
If not, disable the probe to avoid failures (see the
[Maven Plugin Reference](../maven-plugin.md#scalafmt-validation) for details).

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
