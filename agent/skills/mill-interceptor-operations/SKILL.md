---
name: mill-interceptor-operations
description: Run mill-interceptor day-to-day — CLI intercept, Maven commands, inspect-plan, troubleshooting.
---

# mill-interceptor — Operations and troubleshooting

Use for **running builds**, **dry-run inspection**, **CLI forwarding**, and
**diagnosing** failures when Maven + Mill interception misbehaves.

## CLI: explicit parent command

Forward arguments to the underlying tool (Maven must still have the extension
installed if you expect Mill interception for lifecycle phases):

```bash
mill-interceptor intercept mvn test
mill-interceptor intercept maven clean install
mill-interceptor intercept sbt compile
mill-interceptor intercept gradle build
```

`mvn` is an alias for `maven`. This **runs the real binary** (e.g. `mvn`); it does
not replace installing the Maven core extension.

## Maven: common commands

```bash
mvn compile
mvn test
mvn package
mvn mill-interceptor:inspect-plan    # dry-run: show Mill commands + cwd, no subprocess
mvn mill-interceptor:describe        # list plugin goals
```

## When things go wrong

1. **Confirm the extension loads** — `mvn mill-interceptor:describe` should run.
2. **Inspect the plan** — `mvn mill-interceptor:inspect-plan` shows resolved Mill
   command lines and working directory (not env vars).
3. **Strict mode / unmapped phase** — if `mode: strict`, add or fix `lifecycle` /
   `goals` in config so every invoked phase maps to real Mill targets.
4. **Mill not found** — set `mill.executable` or add `mill` / `millw` at repo root;
   the plugin searches common launcher paths when executable is `mill`.
5. **Env / JVM** — set keys under `mill.environment` (see configuration skill).
6. **Publish failures** — ensure Mill has a publish-capable module and credentials
   as for a normal Mill publish.

## Shims (optional)

```bash
mill-interceptor shim generate --help
```

Generates wrapper scripts for maven/gradle/sbt; useful for teams standardizing
entrypoints.

## References (upstream repository)

- `docs/_docs/guides/maven/debugging-with-inspect.md`
- `docs/_docs/usage.md`
- `README.md`
