# mill-interceptor

[![CI](https://github.com/Eleven19/mill-interceptor/actions/workflows/ci.yml/badge.svg)](https://github.com/Eleven19/mill-interceptor/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.eleven19.mill-interceptor/milli_3)](https://central.sonatype.com/artifact/io.eleven19.mill-interceptor/milli_3)
[![GitHub Release](https://img.shields.io/github/v/release/Eleven19/mill-interceptor)](https://github.com/Eleven19/mill-interceptor/releases/latest)

A tool for intercepting other build tools using mill.
The idea is to use mill to "impersonate" other build tools.

## What does it do?

This tool serves as a replacement* for:

- mvn
- gradle
- sbt 

## Usage

Invoke the interceptor through the explicit parent command:

- `mill-interceptor intercept mvn test`
- `mill-interceptor intercept maven clean install`
- `mill-interceptor intercept sbt compile`
- `mill-interceptor intercept gradle build`

`mvn` is accepted as an alias for `maven`.

## Maven plugin

The repository also publishes `mill-interceptor-maven-plugin`, which lets Maven forward into Mill through a core extension loaded from `.mvn/extensions.xml`.

Minimal setup:

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

With only that extension file, the current conventional baseline supports:

- `mvn clean`
- `mvn validate`
- `mvn compile`
- `mvn test`
- `mvn package`
- `mvn verify`
- `mvn mill-interceptor:inspect-plan`

`mvn install` and `mvn deploy` are supported when the Mill build exposes a publish-capable surface such as `PublishModule`.

Add `mill-interceptor.yaml` or `mill-interceptor.pkl` only when you need overrides:

- custom lifecycle mappings
- module-local overrides
- disabling the validate Scalafmt hook
- custom Mill executable, working directory, or environment

Config discovery:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`
- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

Detailed plugin setup, override examples, and fixture-backed behavior live in [docs/usage/maven-plugin.md](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-okw-8-fixtures-docs/docs/usage/maven-plugin.md).

## Launcher scripts

Releases also publish `milli` and `milli.bat` launchers. By default they prefer platform-native artifacts, fall back to the published assembly jar artifact (`milli-dist`) when native artifacts are unavailable, prefer Maven Central as the source, and fall back to GitHub Releases automatically.

Launcher controls:

- `MILLI_VERSION`
- `.mill-interceptor-version`
- `.config/mill-interceptor-version`
- `MILLI_LAUNCHER_MODE` with `native` or `dist`
- `MILLI_LAUNCHER_SOURCE` with `maven` or `github`
- `MILLI_LAUNCHER_USE_NETRC=1` to enable `.netrc` for `curl` or `wget`

## Should I use this?

Sure, if you find it useful.
I had a very particular use-case involving integration with certain build pipelines that were
setup to work with common tools like maven, gradle, and sbt, but with no native support for mill.

## Release artifacts

GitHub Releases publish:

- platform-specific native archives named for `mise`'s GitHub backend
- an executable assembly jar
- `milli`
- `milli.bat`

Maven Central publishes the `milli` artifact family under `io.eleven19.mill-interceptor`:

- `milli`
- `milli-dist`
- platform-specific `milli-native-*` artifacts

Stable releases use tags like `v1.2.3`; prereleases use semantic prerelease tags like `v1.2.3-rc.1`. Maintainer details live in `docs/contributing/releasing.md`.
