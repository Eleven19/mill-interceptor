---
name: mill-interceptor-setup
description: Install mill-interceptor, the milli launcher, and prerequisites for Maven or CLI workflows.
---

# mill-interceptor — Setup and installation

Use when the user needs to **install**, **bootstrap**, or **verify prerequisites** for
mill-interceptor (not Maven lifecycle details or YAML tuning — see the other
mill-interceptor skills).

## What this project is

- **`mill-interceptor`** — CLI that can forward to Maven, sbt, or Gradle; includes
  `mill-interceptor maven setup` and `mill-interceptor shim generate`.
- **`milli`** — shell/Node launchers that download and run the published CLI (native
  binary or assembly), with optional version pins via files or env vars.
- **`mill-interceptor-maven-plugin`** — Maven **core extension** (`.mvn/extensions.xml`)
  that maps Maven lifecycle phases to **Mill** subprocess invocations.

**Docs site:** https://eleven19.github.io/mill-interceptor/

## Prerequisites (typical)

- **JDK** appropriate for Mill and the user’s project.
- **Mill** available as `mill` / `millw` at the repo root (or configured later in
  `mill-interceptor` config), when using the Maven extension path.
- **Maven 3.9+** when using the Maven extension.

## Install the CLI

**From Maven Central (Scala 3 artifact):**

- Group: `io.eleven19.mill-interceptor`
- Artifact: `milli_3` (see current coordinates in upstream README / Central).

Use the consuming build tool’s normal dependency or `cs launch` / coursier patterns
if that fits the user’s workflow.

**From GitHub Releases:** download platform archives, assembly jar, `milli`,
`milli.bat`, and/or `milli.mjs` from the release matching the desired version.

## milli launcher behavior (summary)

Launchers resolve a runnable CLI (prefer native, fall back to `milli-dist` assembly)
and can prefer Maven Central then GitHub Releases.

Common controls (see upstream `README.md`):

- `MILLI_VERSION`
- `.mill-interceptor-version` or `.config/mill-interceptor-version`
- `MILLI_LAUNCHER_MODE` — `native` or `dist`
- `MILLI_LAUNCHER_SOURCE` — `maven` or `github`
- `MILLI_LAUNCHER_USE_NETRC=1` for download auth

## Scaffold Maven extension files (optional)

From a checkout that has `mill-interceptor` on the PATH (or via `milli`):

```bash
mill-interceptor maven setup
# or: mill-interceptor maven setup --dry-run --format yaml
```

This generates `.mvn/extensions.xml` and starter config; use `--extension-version`
if the tool cannot infer the version.

## Next steps

- Maven-only wiring and lifecycle: `agent/skills/mill-interceptor-maven-extension/SKILL.md`
- YAML/Pkl and `mill.environment`: `agent/skills/mill-interceptor-configuration/SKILL.md`
- Day-to-day commands and debugging: `agent/skills/mill-interceptor-operations/SKILL.md`

## References (upstream repository)

- `README.md`
- `docs/_docs/getting-started.md`
- `docs/_docs/guides/release-artifacts.md`
