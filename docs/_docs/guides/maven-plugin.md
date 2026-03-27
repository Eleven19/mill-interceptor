# Maven Plugin Guide

`mill-interceptor-maven-plugin` allows Maven to forward lifecycle phases into Mill through a core extension loaded from `.mvn/extensions.xml`.

## Minimal setup

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 https://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>io.eleven19.mill-interceptor</groupId>
    <artifactId>mill-interceptor-maven-plugin</artifactId>
    <version>${mill.interceptor.version}</version>
  </extension>
</extensions>
```

With only this extension file, the conventional baseline supports:

- `mvn clean`
- `mvn validate`
- `mvn compile`
- `mvn test`
- `mvn package`
- `mvn verify`
- `mvn mill-interceptor:inspect-plan`

`install` and `deploy` are supported when the Mill build exposes a publish-capable surface such as `PublishModule`.

## Baseline lifecycle mapping

- `clean` -> `mill clean`
- `validate` -> optional Scalafmt check via `__.checkFormat`
- `compile` -> `mill compile`
- `test` -> `mill compile test`
- `package` -> `mill compile test jar`
- `verify` -> validate hook + `mill compile test`
- `install` -> `mill compile test jar publishM2Local`
- `deploy` -> `mill compile test jar publish`

## When to add config

Add config files when:

- target names differ from the baseline
- repo-level lifecycle overrides are needed
- module-level behavior must differ from repo defaults
- validate-phase formatting checks should be disabled
- Mill executable/working-dir/env must be customized

Config discovery order:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`
- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

## Strict failure model

Strict mode is the default. Invalid or unavailable configured targets fail fast with the forwarded Mill command and exit code.
