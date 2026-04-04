---
name: mill-interceptor-configuration
description: Author mill-interceptor.yaml or Pkl config — discovery order, mill executable, environment, lifecycle overrides.
---

# mill-interceptor — Configuration

Use when the user needs **`mill-interceptor.yaml` / `.pkl`**, **`mill.environment`**,
custom **lifecycle** or **goal** maps, **working directory**, or **validate/scalafmt**
behavior.

## Discovery order

The Maven plugin searches (first match per scope wins; repo + module layers merge):

1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module** config is merged with **repository** config; **module values override**
scalars and replace maps by key as documented.

## Typical YAML shape

```yaml
mode: strict   # fail fast on missing Mill targets (default)

mill:
  executable: mill          # or ./millw, or an absolute path
  workingDirectory: null    # optional relative or absolute override
  environment:
    MILL_VERSION: "0.12.10"
    MILL_OPTS: "-J-Xmx2g"
    JAVA_HOME: "/usr/lib/jvm/21"

validate:
  scalafmtEnabled: true
  scalafmtTarget: null      # optional explicit target

lifecycle:
  compile:
    - compile
  test:
    - compile
    - test

goals:
  inspect-plan:
    - interceptor.inspect
```

Adjust keys to match the project’s Mill targets.

## Environment variables

- **`mill.environment`** supplies **string key → string value** pairs.
- The subprocess **inherits the Maven process environment** (os-lib default), then
  applies this map as **additions/overrides**.
- **Any** variable name is allowed (`MILL_*`, `JAVA_HOME`, `PATH`, etc.); there is
  no special “Mill-only” filter in config.
- **`inspect-plan` does not print** the resolved env map — verify by running a
  short `mvn` goal or inspecting the merged config mentally from YAML.

## Strict mode

With `mode: strict`, unmapped phases or missing targets surface as **failures**
instead of silent skips — align `lifecycle` / `goals` with the project’s Mill graph.

## Pkl

Apple Pkl modules are supported with the same logical fields; evaluation produces
the same overlay model as YAML.

## References (upstream repository)

- `docs/_docs/guides/maven-plugin.md` (full YAML/Pkl reference)
- `docs/_docs/guides/maven/pkl-configuration.md`
- `README.md` (discovery list)
