# Maven Plugin Publishing Design

## Summary

Extend `modules/mill-interceptor-maven-plugin` from a structural placeholder into
a real published Maven plugin artifact. The plugin should publish under the same
group and version family as the existing artifacts, use artifactId
`mill-interceptor-maven-plugin`, include the Maven plugin descriptor metadata
required for Maven consumption, and add integration tests that prove a fixture
project can resolve and execute a placeholder goal.

This remains an incremental pass. The plugin should be consumable by Maven now,
but it should not yet implement YAML mapping, PKL mapping, or lifecycle
interception.

## Goals

- publish `modules/mill-interceptor-maven-plugin` as
  `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin:<version>`
- make the produced jar a real Maven plugin artifact rather than a plain JVM jar
- generate Maven plugin metadata from annotated placeholder Mojos
- add integration tests that run Maven against a fixture project using the local
  plugin coordinates
- update Central publish and metadata verification so the new plugin is part of
  the supported publish surface

## Non-Goals

- implementing real Mill-to-Maven lifecycle mapping
- defining YAML or PKL configuration formats
- attaching the plugin jar to GitHub release assets unless that becomes a later
  requirement
- adding non-placeholder Mojo behavior beyond what is needed to prove packaging
  and invocation

## Approach Options

### 1. Real Maven plugin packaging with annotated Mojos

Make the plugin module publish as a real Maven plugin jar and generate the
descriptor from annotated placeholder Mojos.

Pros:

- produces the correct long-term packaging model immediately
- keeps metadata and code in sync
- avoids a second transition later
- makes integration testing meaningful

Cons:

- requires plugin-specific build wiring now

### 2. Plain jar with hand-written plugin metadata

Publish the module under the right coordinates but hand-maintain the plugin
descriptor under `META-INF/maven/`.

Pros:

- smaller initial implementation

Cons:

- brittle and easy to drift
- does not scale once multiple Mojos appear

### 3. Separate wrapper artifact for plugin metadata

Keep the module as a plain jar and add another artifact that wraps it with the
plugin descriptor.

Pros:

- isolates packaging concerns

Cons:

- needless complexity for the current scope
- splits the plugin surface across artifacts

## Recommended Approach

Use option 1. Publish a real Maven plugin artifact from
`modules/mill-interceptor-maven-plugin`, generate the descriptor from annotated
placeholder Mojos, and verify actual Maven consumption through integration
tests.

## Architecture

### Coordinates

- Group: `io.eleven19.mill-interceptor`
- ArtifactId: `mill-interceptor-maven-plugin`
- Version: same flow as the existing `milli` artifact family

### Module Responsibilities

- `modules/mill-interceptor`
  - remains owner of the current `milli`, `milli-dist`, and `milli-native-*`
    publish surface
- `modules/mill-interceptor-maven-plugin`
  - publishes the Maven plugin artifact
  - contains placeholder Mojo implementations
  - contains plugin unit tests
  - may own a plugin-specific integration-test submodule or local fixture setup

### Placeholder Plugin Surface

The first pass should expose one or more placeholder goals through annotated
Mojos. The goal names should be explicit and stable, but behavior should stay
minimal and unsurprising. A placeholder goal that logs or validates invocation
is sufficient for this phase.

### Build Integration

The Mill build should:

- assign the Maven plugin artifactId explicitly
- ensure the plugin descriptor is generated and packaged
- include the plugin artifact in publish metadata and Central publish tasks
- leave GitHub release assets unchanged unless explicitly expanded later

## Testing And Verification

### Unit Tests

Add or extend tests inside the plugin module to cover:

- placeholder goal metadata
- any small utility code introduced to support plugin packaging

### Integration Tests

Add plugin integration tests that:

- make the plugin available through local publish/install
- run Maven against a small fixture project with a `pom.xml`
- resolve the plugin by `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin`
- execute at least one placeholder goal successfully

### Success Criteria

- `modules.mill-interceptor-maven-plugin.publishSonatypeCentral` resolves as a
  publish task
- publish metadata includes `mill-interceptor-maven-plugin`
- the produced jar is consumable as a Maven plugin
- the fixture Maven build resolves and runs the placeholder goal
- the existing interceptor product tests and publish behavior remain green

## Risks

- Maven plugin packaging has stricter metadata expectations than the current JVM
  artifacts
- integration tests may introduce local Maven repository assumptions that need
  to be made explicit
- placeholder Mojos must be minimal enough to avoid implying finalized plugin
  semantics
