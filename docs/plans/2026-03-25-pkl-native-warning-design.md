# PKL Native Warning Design

## Goal

Suppress or resolve the JDK 25 native-access warning emitted during Maven plugin tests that load PKL-backed configuration.

## Current State

The warning originates when PKL evaluation initializes Graal/Truffle native loading during tests. The relevant code path is:

- `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/PklConfigEvaluator.scala`
- `modules/mill-interceptor-maven-plugin/src/io/eleven19/mill/interceptor/maven/plugin/config/ConfigLoader.scala`

The warning currently shows up in Maven plugin tests, especially the config-loader coverage.

## Recommendation

Prefer a targeted test-runtime suppression first, not a global repo-wide JVM flag and not an unnecessary production-code workaround.

The ordering should be:

1. confirm which test task emits the warning
2. see whether the PKL evaluator can be configured cleanly
3. if not, apply a Maven-plugin-test-scoped JVM flag or env configuration
4. keep the fix local to the affected module/tests

## Why This Scope

The issue is noisy but low-risk. The right fix should:

- remove warning noise from CI and local tests
- avoid changing production semantics unless necessary
- avoid broad JVM-wide flags that affect unrelated modules

## Risks

The main risk is suppressing the wrong thing globally. A narrow fix in the Maven plugin test surface is preferred unless upstream PKL/Graal offers a better supported knob.

## Acceptance

This task is complete when:

- the Maven plugin test suite no longer emits the PKL native-access warning on JDK 25
- the fix is scoped to the affected module/tests unless a production-scoped change is clearly justified
- the relevant tests still pass normally
