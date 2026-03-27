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
