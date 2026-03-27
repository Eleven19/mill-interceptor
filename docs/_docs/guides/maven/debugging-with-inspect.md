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
