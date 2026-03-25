# Shared Execution Plan Design

## Goal

Evaluate and, if the dependency direction stays clean, extract the neutral execution-plan model from the Maven plugin into a shareable domain layer that both the Maven plugin and CLI can use without duplicating planning concepts.

## Current State

The Maven plugin currently owns the neutral-looking model:

- `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/model/MillExecutionPlan.scala`
- `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/resolve/ExecutionPlanResolver.scala`
- `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/exec/MillRunner.scala`

The CLI does not currently depend on these plan types. It generates Maven setup files, but does not yet share the execution-planning domain with the plugin.

## Recommendation

Extract only the neutral model types first:

- `ExecutionRequest`
- `ExecutionRequestKind`
- `MillExecutionPlan`
- `PlanStep`

Do not extract Maven-specific adapters, resolver policy, or runner behavior in this task.

## Why This Scope

This is the smallest change that meaningfully answers `MI-szk`:

- it proves whether the dependency direction can remain clean
- it avoids prematurely creating a large shared execution package
- it keeps Maven-specific planning logic in the plugin where it already belongs
- it gives the CLI a stable domain surface to adopt later without forcing immediate functional changes

## Proposed Ownership

Move the neutral types into the CLI-facing core module:

- new package under `modules/mill-interceptor/src/io/eleven19/mill/interceptor/model/`

The Maven plugin then imports those shared types instead of defining them locally.

This direction is preferable to creating a third module right now because:

- the CLI module already represents the non-Maven product surface
- the shared types are conceptually closer to interceptor-core behavior than to Maven integration
- a new module would add more build churn than this evaluation needs

## Boundaries

Keep local to the Maven plugin:

- `ExecutionPlanResolver`
- lifecycle baseline policy
- Maven request translation
- `MillRunner`
- Mojo failure mapping

Only the type model should move in this task.

## Risks

The main risk is accidental reverse dependency pressure from the CLI module back into Maven plugin code. This task should fail fast if extraction requires pulling Maven-specific config or resolver concepts into the shared package.

## Acceptance

This task is complete when:

- the neutral plan/request types live outside the Maven plugin package
- the Maven plugin builds and tests against the extracted types
- the CLI compiles cleanly with the new shared package in place
- the resulting ownership is documented by the code layout itself, not only by comments
