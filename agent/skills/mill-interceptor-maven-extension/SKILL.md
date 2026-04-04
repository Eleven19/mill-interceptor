---
name: mill-interceptor-maven-extension
description: Configure the mill-interceptor Maven core extension, baseline lifecycle mapping, and extension version.
---

# mill-interceptor — Maven core extension

Use when the user wants **Maven to delegate lifecycle phases to Mill** via
`mill-interceptor-maven-plugin`, or needs help with **`.mvn/extensions.xml`**,
**baseline goals**, or **publish (`install` / `deploy`)** requirements.

## Mental model

- Registration is via **`.mvn/extensions.xml` only** — no `<plugin>` block required in
  `pom.xml` for the minimal path.
- On each bound phase, the extension resolves a **Mill execution plan** (baseline
  plus optional config) and runs **`mill` <targets>** as a subprocess from the
  configured working directory.

## Minimal extension snippet

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

Pin `${mill.interceptor.version}` in the root POM (or replace with a literal
version).

## Default lifecycle → Mill mapping (baseline)

| Maven phase | Mill targets (typical) |
|-------------|-------------------------|
| clean | `clean` |
| validate | scalafmt probe if enabled (see docs) |
| compile | `compile` |
| test | `compile test` |
| package | `compile test jar` |
| verify | `compile test` |
| install | `compile test jar publishM2Local` |
| deploy | `compile test jar publish` |

Phases run in order; earlier phases are included when a later phase is requested
(e.g. `package` runs validate → … → package).

**Publish phases** need a Mill module that supports publishing (e.g.
`PublishModule`).

## Operational goals (invoke explicitly)

```bash
mvn mill-interceptor:describe
mvn mill-interceptor:inspect-plan
```

## Generator command

```bash
mill-interceptor maven setup [--dry-run] [--format yaml|pkl] [--extension-version V] [--force]
```

## Multi-module / reactor

Repository- and module-scoped config merge (module wins). See published guides for
reactor behavior and overrides.

## References (upstream repository)

- `docs/_docs/guides/maven-plugin.md`
- `docs/_docs/guides/maven/getting-started.md`
- `docs/_docs/guides/maven/multi-module.md`
