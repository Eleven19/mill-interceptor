# Maven Core Extension Activation Design

## Summary

Implement real Maven core-extension activation for
`mill-interceptor-maven-plugin` so a repository can add only
`.mvn/extensions.xml` and have the common Maven lifecycle intercepted and
forwarded to Mill.

This is the missing behavior exposed by the new `MI-okw.8` extension-only
fixture: the current artifact can be installed locally, but Maven does not yet
discover or activate the plugin behavior from `.mvn/extensions.xml` alone.

## Goals

- make one artifact load as a true Maven core extension
- require only `.mvn/extensions.xml` for the common lifecycle path
- intercept the common lifecycle without any plugin declaration in `pom.xml`
- keep using the existing config loader, resolver, and Kyo-native runner
- make the operational surface, especially `inspect-plan`, available under the
  same minimal setup model

## Non-Goals

- arbitrary third-party plugin-goal interception in this phase
- duplicate planning or execution logic separate from the current resolver and
  runner
- documenting the plugin as complete before extension-only activation is real

## Recommended Approach

Use one artifact that carries both:

- Maven core-extension activation behavior
- the plugin surface and operational goals

The extension path becomes the primary product path. The current Mojo layer
stays useful as an adapter and operational surface, but it should not be the
thing users must bind manually in their POMs.

## Activation Model

The intended user flow should be:

1. make the artifact resolvable
2. add it to `.mvn/extensions.xml`
3. run Maven normally

No plugin declaration in `pom.xml` should be required for:

- `mvn clean`
- `mvn validate`
- `mvn compile`
- `mvn test`
- `mvn package`
- `mvn verify`
- `mvn install`
- `mvn deploy`
- `mvn mill-interceptor:inspect-plan`

If operational goals still require a separate declaration, that should be
treated as an implementation gap, not as acceptable final behavior.

## Architecture

The implementation should be split into three internal layers:

### 1. Extension Bootstrap

- package the artifact with the metadata Maven expects for a core extension
- ensure Maven loads it early from `.mvn/extensions.xml`

### 2. Lifecycle Interception Adapter

- observe the requested Maven lifecycle phase
- suppress or replace native Maven behavior for the supported lifecycle set
- translate Maven session and project state into the existing neutral
  `ExecutionRequest`
- call the existing config loader, `ExecutionPlanResolver`, and `MillRunner`

### 3. Operational Goal Availability

- make `inspect-plan` reachable under the same minimal setup model
- avoid creating a second planning stack just for operational commands

## Lifecycle Interception Boundary

The first supported lifecycle set remains:

- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `install`
- `deploy`

The extension should take control of these paths directly. This is more invasive
than ordinary plugin bindings, but it is the only path that honestly matches
the desired “Maven as entrypoint to Mill” behavior with minimal setup.

## Reuse Rule

Do not duplicate planning logic.

The extension path should reuse:

- existing config discovery and loading
- existing lifecycle baseline and overrides
- existing `MillExecutionPlan`
- existing Kyo-native `MillRunner`

The current Mojo code should remain a thin boundary and helper surface, not a
second independent implementation.

## Verification

This feature is only complete when the extension-only fixture works end to end.

Minimum proof:

- install the artifact locally
- add only `.mvn/extensions.xml`
- run the common lifecycle commands
- run `mvn mill-interceptor:inspect-plan`
- confirm the results come from Mill forwarding behavior rather than native
  Maven defaults

## Risks

- Maven core-extension hooks may not expose the exact lifecycle interception
  seam we want without deeper internals work
- operational goal discovery may differ from lifecycle interception
- extension packaging may need additional metadata beyond the current plugin jar
  layout
- reactor behavior may surface lifecycle assumptions not visible in the current
  single-module tests

## Relationship To MI-okw.8

`MI-ajn` is a real blocker for `MI-okw.8`.

Until this activation model works:

- the minimal extension-only fixture cannot pass honestly
- the README cannot truthfully claim that `.mvn/extensions.xml` alone is enough
- fixture and documentation work should be treated as partial
