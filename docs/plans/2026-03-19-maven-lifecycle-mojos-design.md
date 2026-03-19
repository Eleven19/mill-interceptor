# Maven Lifecycle Mojos Design

## Summary

Implement the Maven-facing boundary for the real `mill-interceptor-maven-plugin`
so a repository can activate one extension artifact through
`.mvn/extensions.xml` and run the common Maven lifecycle against Mill with very
little Maven-side configuration.

This phase does not attempt deep interception of arbitrary direct plugin-goal
invocations. It focuses on the common lifecycle first and leaves richer goal
interception to a later phase.

## Goals

- replace the placeholder Mojo surface with real lifecycle Mojos
- keep activation low-friction through one extension artifact plus
  `.mvn/extensions.xml`
- translate Maven execution into the existing neutral `ExecutionRequest`
- resolve through the existing config and lifecycle-baseline layers
- execute through the existing Kyo-native `MillRunner`
- surface failures as Maven-friendly diagnostics
- keep minimal configuration genuinely small for common Mill repos

## Non-Goals

- intercepting arbitrary third-party plugin goals in this phase
- reworking the resolver or runner architecture already implemented
- broad end-to-end fixture coverage and user-facing docs
  - tracked by `MI-okw.8`
- CLI generation of Maven setup
  - tracked by `MI-8l6`

## Recommended Approach

Use one artifact that acts as both the Maven plugin and the core extension. The
repo activates it through `.mvn/extensions.xml`, while the artifact exposes a
real set of lifecycle Mojos plus an operational `inspect-plan` goal.

Internally, keep the Mojos thin:

- capture Maven session and project state
- build a neutral `ExecutionRequest`
- load effective config
- resolve a `MillExecutionPlan`
- either execute it or render it for inspection

This keeps the Maven boundary conventional enough to test while still moving
toward the desired "Maven as entrypoint to Mill" behavior.

## Architecture

The implementation should stay layered:

- extension and plugin packaging layer
  - one published artifact
  - activated through `.mvn/extensions.xml`
- Mojo adapter layer
  - phase-specific Mojos for the common lifecycle
  - operational goal(s) like `inspect-plan`
- request translation layer
  - Maven session and project state to `ExecutionRequest`
- resolution layer
  - existing `ExecutionPlanResolver`
- execution layer
  - existing `MillRunner`

### Lifecycle Surface

Phase 1 should expose real support for:

- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `install`
- `deploy`

It should also expose:

- `describe`
- `inspect-plan`

The lifecycle Mojos should be separate concrete classes, but they should share a
single abstract adapter so the Maven boundary logic is implemented once.

## Activation Model

The first supported activation path should be:

1. install the plugin artifact
2. add it to `.mvn/extensions.xml`
3. optionally add `mill-interceptor.yaml` or `mill-interceptor.pkl`
4. run Maven lifecycle commands normally

This is still a small amount of Maven configuration, but it is much closer to
"just works" than requiring full plugin execution bindings in every `pom.xml`.

## Mojo Boundary

The shared abstract adapter should be responsible for:

- reading Maven project and session details
- identifying the requested lifecycle phase or operational goal
- collecting relevant user properties
- converting that state into a neutral `ExecutionRequest`
- invoking config loading, resolution, and execution
- converting plan failures and runner failures into Maven exceptions

Each concrete lifecycle Mojo should only declare:

- goal name
- default phase where relevant
- whether it executes the plan or only inspects it

## Diagnostics

Failure reporting should stay explicit and actionable:

- strict plan fail-steps become Maven failures with guidance
- probe failures mention the missing target and how to inspect available targets
- invocation failures mention the Mill command and exit code
- `inspect-plan` should render the ordered plan truthfully rather than inventing
  synthetic output

## Minimal Configuration Expectations

The happy path should require no explicit lifecycle mapping for common repos.
That only works if the conventional lifecycle baseline remains the default seed
for supported phases.

Users should only need config when they want to:

- override default phase mappings
- disable hooks like the `validate` Scalafmt probe
- customize executable, environment, or working directory
- add goal mappings beyond the common lifecycle

If common Mill repos still need a large config file to get going, that should be
treated as a design defect in the baseline, not as acceptable documentation
burden.

## Testing Strategy

`MI-okw.7` should focus on the Maven boundary itself:

- unit tests for goal registry and lifecycle Mojo coverage
- unit tests for Maven-to-`ExecutionRequest` translation
- unit tests for exception mapping from plan and runner failures
- unit tests for `inspect-plan` output using the real resolver and dry-run
  renderer

End-to-end Maven fixture coverage belongs in `MI-okw.8`, once the real Mojo
surface is in place.

## Follow-Up Work

- `MI-okw.8`
  - end-to-end Maven lifecycle fixtures and docs
- `MI-8l6`
  - CLI-guided Maven plugin setup generation
- later phase of `MI-okw`
  - direct plugin-goal interception beyond the common lifecycle
