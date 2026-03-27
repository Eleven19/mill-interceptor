# Usage

Invoke the interceptor through the explicit parent command:

- `mill-interceptor intercept mvn test`
- `mill-interceptor intercept maven clean install`
- `mill-interceptor intercept sbt compile`
- `mill-interceptor intercept gradle build`

## Configuration discovery

Config files are discovered in this order:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`
- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

Use configuration when you need:

- custom lifecycle mappings
- module-local overrides
- custom Mill executable, working directory, or environment
- opt-out of validate-phase Scalafmt checks

## Maven plugin forwarding

For full extension-based Maven setup and lifecycle behavior, see [Maven Plugin Guide](guides/maven-plugin.md).

## Release and launcher behavior

For release artifacts and launcher controls, see [Release Artifacts](guides/release-artifacts.md).
