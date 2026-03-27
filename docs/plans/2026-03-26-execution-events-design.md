# Execution Events Design

## Goal

Add a small shared execution-event model that both the CLI and Maven plugin can
build on without introducing a mediator or event bus.

## Problem

The current shared execution boundary has:

- shared request and plan types in `modules/mill-interceptor`
- Maven-side resolution in `ExecutionPlanResolver`
- Maven-side execution in `MillRunner`
- result types that describe final success or failure

What it does not have is a shared vocabulary for the execution lifecycle
between plan resolution and final runner results. That leaves later diagnostics,
test instrumentation, and future CLI progress output leaning on ad hoc
inspection of results and dry-run structures.

## Decision

Introduce a small shared public event ADT in the same neutral model layer as
`MillExecutionPlan`.

Recommended shared types:

- `ExecutionEvent`
- `PlanResolved`
- `StepStarted`
- `StepFinished`
- `StepFailed`
- `ExecutionEventSink`

The event model is public because:

- it sits next to already shared request/plan types
- it is naturally reusable by both the CLI and Maven plugin
- delaying publication would likely create duplicated internal shapes later

The scope stays intentionally narrow:

- no mediator
- no async bus
- no transport or serialization layer
- no timestamps or correlation IDs unless a concrete caller needs them

## Boundary

Events describe what happened. They do not control execution.

Resolver responsibilities:

- continue returning `MillExecutionPlan`
- emit `PlanResolved` through a sink

Runner responsibilities:

- continue returning `RunnerResult`
- emit step lifecycle events while executing:
  - `StepStarted`
  - `StepFinished`
  - `StepFailed`

Callers:

- Maven plugin can keep ignoring events at first
- tests can use a recording sink immediately
- CLI can adopt the event stream later without forcing another model extraction

## Integration Strategy

Add a minimal sink abstraction:

- `ExecutionEventSink.publish(event: ExecutionEvent): Unit`

Provide:

- `ExecutionEventSink.noop`
- a simple recording sink for tests

Keep event emission optional by threading the sink as a parameter with a default
no-op implementation at integration points:

- `ExecutionPlanResolver.resolve(..., sink = ExecutionEventSink.noop)`
- `MillRunner.execute(..., sink = ExecutionEventSink.noop)`

This localizes the change to the neutral model plus the two existing execution
boundaries.

## Event Semantics

`PlanResolved`

- emitted once when a resolver produces a `MillExecutionPlan`
- carries the request and the final ordered steps

`StepStarted`

- emitted immediately before a probe or invoke subprocess runs

`StepFinished`

- emitted when a probe or invoke subprocess exits with success

`StepFailed`

- emitted when a subprocess exits non-zero or when a terminal fail-step is hit

The existing `RunnerResult` remains the authoritative overall result. Events are
the execution trace, not a replacement for the result model.

## Testing

Add tests in two areas:

Shared model tests:

- construction/equality of the new event variants
- recording sink captures ordered events

Behavioral emission tests:

- resolver emits `PlanResolved`
- runner emits `StepStarted` and `StepFinished` for successful steps
- runner emits `StepFailed` for non-zero subprocesses
- runner emits `StepFailed` for `PlanStep.Fail`
- no-op sink preserves current behavior

## Risks

The main risk is overdesign. The mitigation is keeping the public shape tiny and
avoiding any mediator or observability infrastructure in this task.

Another risk is producing two parallel explanations of execution:

- event stream
- `RunnerResult`

That is acceptable because they answer different questions:

- events: what happened along the way
- result: what the final outcome was
