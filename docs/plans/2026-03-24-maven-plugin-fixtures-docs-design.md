# Maven Plugin Fixtures And Docs Design

## Summary

Implement the final proof and documentation layer for the real
`mill-interceptor-maven-plugin`.

This phase adds checked-in Maven fixtures that exercise the plugin end to end
and updates the user-facing documentation so the plugin can be adopted without
reverse-engineering tests or code.

## Goals

- prove the common Maven lifecycle end to end through real Maven fixtures
- prove the extension-first activation path through `.mvn/extensions.xml`
- document a minimal setup that needs no YAML or PKL for the conventional
  lifecycle baseline
- document override and layering behavior with repo-level and module-level
  config examples
- document `inspect-plan` and the validate-phase Scalafmt hook

## Non-Goals

- introducing new lifecycle behavior beyond what `MI-okw.7` already enabled
- deep arbitrary plugin-goal interception
- CLI-guided plugin setup generation
  - tracked separately by `MI-8l6`

## Recommended Approach

Use checked-in fixture directories as the source of truth and copy them into
temporary directories during tests.

This gives the repo:

- stable, reviewable fixture inputs
- examples that double as documentation
- safe runtime isolation for Maven local repository and build output mutations

## Fixture Architecture

This phase should add two fixture families under the Maven plugin integration
test resources:

### 1. Minimal Single-Module Fixture

Purpose:

- prove the conventional lifecycle baseline
- prove the low-friction activation path
- prove that no YAML or PKL is required for common lifecycle use

Expected characteristics:

- `.mvn/extensions.xml` present
- no `mill-interceptor.yaml`
- no `mill-interceptor.pkl`
- simple Mill build layout

### 2. Multi-Module Override Fixture

Purpose:

- prove repo-level defaults plus module-local overrides
- prove deterministic layering behavior
- give users a concrete customization example

Expected characteristics:

- repo-level plugin config
- module-local plugin config override
- at least one phase mapping overridden in a child module

### Strict Failure Coverage

Strict-failure coverage should not pollute the happy-path fixtures.

Use either:

- a dedicated tiny failure fixture, or
- a separate failing invocation against the override fixture

so the healthy examples stay readable.

## Coverage Matrix

### Minimal Fixture

The minimal fixture should cover:

- `mvn clean`
- `mvn validate`
- `mvn compile`
- `mvn test`
- `mvn package`
- `mvn verify`
- `mvn install`
- `mvn deploy`
- `mvn mill-interceptor:inspect-plan`

The important assertion is not just exit code success. The fixture should prove
that the plugin actually forwards to Mill and that the conventional baseline is
usable without extra config.

## Baseline Realism

The minimal fixture should use a realistic Mill layout rather than toy
top-level targets.

The conventional lifecycle baseline should prefer Mill query syntax where it
creates a better no-config default for real builds:

- `clean` -> `clean`
- validate Scalafmt hook -> `__.checkFormat`
- `compile` -> `__.compile`
- `test` -> `__.test`

This makes the fixture a design probe as well as a test:

- if query-based defaults work well for realistic module layouts, the baseline
  is strong
- if `package`, `install`, or `deploy` still require too much explicit config,
  that should be treated as signal to revisit the baseline rather than merely
  documenting complexity

`verify` can continue to reflect validate-hook behavior plus test execution.

### Override Fixture

The multi-module fixture should cover:

- repo-level override config
- module-local override config
- one or more child-module phase overrides
- optional disablement of the validate Scalafmt hook in at least one path
- one strict failure path with clear diagnostics

## Documentation

The README should gain a real Maven plugin usage section covering:

- what the plugin is for
- the minimal setup through `.mvn/extensions.xml`
- the common lifecycle phases that work by default
- `inspect-plan`
- the validate-phase Scalafmt hook and its disable path
- when config is optional versus required

The deeper reference should live in a user-facing doc rather than only a
maintainer doc. Recommended path:

- `docs/usage/maven-plugin.md`

That doc should include:

- extension activation example
- minimal extension-only setup
- repo-level YAML or PKL override example
- module-level override example
- config discovery locations

## Testing Strategy

This phase should extend the existing Maven integration test suite rather than
replace it.

The tests should:

- install the locally-built plugin into a temporary Maven repo
- copy fixture directories into isolated temp directories
- run Maven commands against those temp copies
- assert both successful lifecycle forwarding and strict failures

Keep fixture execution explicit and readable. It is better to have a handful of
clear Maven invocations than one large opaque helper that hides what behavior is
being verified.

## Success Criteria

- a minimal fixture works with only `.mvn/extensions.xml`
- a multi-module fixture proves repo and module override layering
- strict failure behavior is exercised end to end
- README documents actual Maven plugin usage
- a dedicated user-facing doc explains minimal setup, lifecycle defaults,
  overrides, and config discovery
