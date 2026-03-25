# Maven Plugin

`mill-interceptor-maven-plugin` lets Maven act as an entrypoint into a Mill build.

## Minimal setup

The smallest supported setup is a project-local Maven core extension file:

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

With only `.mvn/extensions.xml`, the conventional baseline supports:

- `mvn clean`
- `mvn validate`
- `mvn compile`
- `mvn test`
- `mvn package`
- `mvn verify`
- `mvn mill-interceptor:inspect-plan`

The fixture at [minimal-lifecycle](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-okw-8-fixtures-docs/modules/mill-interceptor-maven-plugin/itest/resources/fixtures/minimal-lifecycle) is the reference example for that path.

## Conventional baseline

The baseline is strict-first, but it starts from predictable Mill defaults so common Maven lifecycle commands do not require interceptor config.

Current defaults:

- `clean` -> `mill clean`
- `validate` -> optional Scalafmt verification via `__.checkFormat`
- `compile` -> `__.compile`
- `test` -> `__.test`
- `package` -> `compile test jar`
- `verify` -> validate hook plus `test`
- `install` -> `compile test jar publishM2Local`
- `deploy` -> `compile test jar publish`

`install` and `deploy` assume the Mill build exposes a publish-capable surface. In practice that means a module that mixes in `PublishModule` or an equivalent publish task surface.

The fixture at [publish-lifecycle](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-okw-8-fixtures-docs/modules/mill-interceptor-maven-plugin/itest/resources/fixtures/publish-lifecycle) is the reference example for the publish path.

## When config is needed

Add config when:

- your Mill target names differ from the conventional baseline
- you want repo-level lifecycle overrides
- one Maven module needs a different mapping than the repo default
- you want to disable the validate-phase Scalafmt hook
- you need a non-default Mill executable, working directory, or environment

Config discovery is deterministic:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`
- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

Repository-level files apply first, then module-level files overlay them.

## Example overrides

Repo-level override:

```yaml
validate:
  scalafmtEnabled: false
lifecycle:
  compile:
    - __.compile
```

Module-local override:

```yaml
lifecycle:
  compile:
    - app.compile
```

The fixture at [multi-module-overrides](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-okw-8-fixtures-docs/modules/mill-interceptor-maven-plugin/itest/resources/fixtures/multi-module-overrides) demonstrates repo defaults overridden by a module-local config file.

## Failure behavior

Strict mode is the default. If a configured target is wrong or unavailable, Maven fails with the forwarded Mill command and exit code instead of silently falling back.

The fixture at [strict-failure](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-okw-8-fixtures-docs/modules/mill-interceptor-maven-plugin/itest/resources/fixtures/strict-failure) demonstrates that failure path.
