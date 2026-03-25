# Maven Publish Fixture Design

## Context

`MI-okw.8` established an honest zero-config baseline for Maven lifecycle forwarding
through `.mvn/extensions.xml` alone:

- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `mill-interceptor:inspect-plan`

That minimal fixture intentionally used a realistic Mill `ScalaModule` build
surface without YAML or PKL configuration.

While extending the fixture toward `install` and `deploy`, the build exposed a
real contract boundary: a plain realistic `ScalaModule` fixture does not provide
Mill publish tasks, so Maven publish-related phases cannot be claimed honestly
without a publish-capable Mill surface.

`MI-biw` exists to make that contract explicit, test it end to end, and keep the
README honest.

## Goal

Prove Maven `install` and `deploy` forwarding through the core-extension path
when, and only when, the Mill build exposes a minimum publish-capable surface.

Preferred outcome:

- `.mvn/extensions.xml` remains the only interceptor-specific setup
- the fixture gains a realistic `PublishModule`-capable Mill build
- `mvn install` and `mvn deploy` succeed through the interceptor path without
  `mill-interceptor.yaml|pkl`

Fallback outcome:

- if a zero-config publish path is not viable even with `PublishModule`, use the
  smallest possible interceptor config override and document that contract
  explicitly

## Recommendation

Add a second checked-in integration fixture, `publish-lifecycle`, dedicated to
publish semantics.

Why this is the right split:

- it keeps the current zero-config lifecycle fixture clean and readable
- it avoids pretending every Mill build supports Maven publish phases
- it gives the docs a concrete example of the minimum publish-capable surface
- it lets the fixture drive the baseline decision instead of guessing from API
  names

## Fixture Contract

The publish-capable fixture should:

- use `.mvn/extensions.xml`
- use a realistic `build.sc`
- expose a Mill module that mixes in `PublishModule`
- provide the minimum publish metadata necessary for Mill to surface the real
  publish tasks
- publish only to local, fixture-owned destinations during tests

The first implementation attempt should avoid `mill-interceptor.yaml|pkl`.

Only if the publish-capable fixture proves that the current baseline still
cannot reach the correct publish tasks should we add a tiny override file.

## Acceptance Coverage

The publish-capable fixture should prove:

- `mvn install`
- `mvn deploy`

And it should prove them honestly:

- through `.mvn/extensions.xml` activation
- through the interceptor path, not native Maven success-by-accident
- with observable publish side effects in local fixture-owned destinations

## Baseline Decision

The current baseline assumptions are:

- `install` -> `publishLocal`
- `deploy` -> `publish`

This task should not assume those mappings are correct.

Instead, it should let the fixture answer:

1. whether a realistic `PublishModule` build exposes those task names at all
2. whether the correct mapping uses bare targets or query-style targets
3. whether zero interceptor config remains viable for publish phases

The fixture result, not the current baseline, should determine the final
mapping and documentation.

## Documentation Impact

The final docs should state the contract precisely:

- `clean` through `verify` work against the minimal realistic baseline with no
  interceptor config
- `install` and `deploy` require a `PublishModule`-capable Mill surface
- interceptor config remains optional unless the publish fixture proves
  otherwise
