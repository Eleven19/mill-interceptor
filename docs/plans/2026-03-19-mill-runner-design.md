# Mill Runner Design

## Summary

Implement `MI-okw.6` by adding a Kyo-native runner that executes
`MillExecutionPlan` step-by-step through real Mill subprocesses while streaming
stdout and stderr live.

## Goals

- execute `PlanStep` values without shell-string indirection
- stream process output live during probe and invocation steps
- keep a structured execution summary for later Mojo diagnostics
- honor configured Mill executable, environment, and working directory
- support dry-run rendering without spawning subprocesses
- preserve typed failure information for fail, probe, and invocation steps

## Non-Goals

- translating Maven session state into execution requests
- replacing placeholder Mojos
- broad integration fixtures beyond the runner boundary
- extracting runner types into a shared module

## Recommended Approach

Treat the runner as a step executor over `MillExecutionPlan`.

Step mapping:

- `ProbeTarget(target)`
  - run a real subprocess such as `mill resolve <target>`
- `InvokeMill(targets)`
  - run a real subprocess such as `mill <targets...>`
- `Fail(message, guidance)`
  - stop immediately without spawning a subprocess

The runner should stream output live but still return a structured result that
records which steps completed, which command vectors were used, and where
execution stopped.

## Architecture

Add a new execution package under the Maven plugin module.

Suggested pieces:

- `MillRunner`
  - public service entrypoint
- runner result model
  - completed steps
  - success or failure status
  - typed failure details
- command builder helpers
  - probe commands
  - invocation commands
  - working-directory and environment resolution

Keep command construction centralized so later Mojos and inspect-plan logic do
not duplicate process details.

## Command Construction

Inputs:

- `ExecutionRequest`
- `EffectiveConfig`
- `PlanStep`

Rules:

- probe step
  - `Vector(millExecutable, "resolve", target)`
- invocation step
  - `Vector(millExecutable) ++ targets`
- no shell interpolation
- no ad hoc quoting logic

Working directory:

- default to `request.moduleRoot`
- apply configured override deterministically

Environment:

- start from the current process environment
- layer in configured `mill.environment`
- keep this separate from any future Maven-property forwarding

## Output and Diagnostics

The runner should stream stdout and stderr directly while still returning a
summary model.

Suggested result shape:

- `RunnerResult`
  - completed `StepResult`s
  - final status
- `StepResult`
  - step
  - rendered command vector if a subprocess ran
  - exit code if applicable
- `RunnerFailure`
  - fail-step failure
  - probe failure
  - invocation failure

This keeps later Mojo diagnostics and `inspect-plan` support grounded in typed
execution results rather than log scraping.

## Testing

Runner tests should cover:

- dry-run rendering without spawning
- probe command rendering
- invocation command rendering
- environment override handling
- working-directory selection
- fail-step short-circuiting
- non-zero exit handling

Prefer a fake or injected process boundary for most tests so the runner remains
fast and deterministic. A small real-process smoke test is optional if Kyo’s
process API makes it straightforward.
