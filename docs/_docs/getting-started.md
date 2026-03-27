# Getting Started

## What this tool does

`mill-interceptor` provides an explicit parent command that forwards build-tool style commands into Mill:

- `mill-interceptor intercept mvn test`
- `mill-interceptor intercept sbt compile`
- `mill-interceptor intercept gradle build`

`mvn` is accepted as an alias for `maven`.

## Install from Maven Central

Artifacts are published under:

- group: `io.eleven19.mill-interceptor`
- core artifact: `milli_3`

Use your build tool's normal dependency declaration for Scala 3 artifacts.

## Next steps

- Read [Usage](usage.md) for command and configuration behavior.
- Read [Maven Plugin Guide](guides/maven-plugin.md) for Maven lifecycle forwarding through a core extension.
