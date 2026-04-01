# Strict Unmapped Phase Failure Design

## Context

`MI-fuw` covers a specific gap in the Maven plugin integration suite: main does
not currently prove the strict-mode behavior for Maven lifecycle phases that are
explicitly unmapped by configuration.

The closest existing integration coverage is the `strict-failure` fixture in
`modules/mill-interceptor-maven-plugin/itest`, but that fixture exercises a
different contract. It points `validate.scalafmtTarget` at a missing Mill
target, which proves that unavailable configured targets fail, not that strict
mode rejects an explicit `unmapped` lifecycle mapping with a clear diagnostic.

The issue asks for a fixture pattern adapted from the older
`mi-ibm-maven-lifecycle-forwarding` work and requires `.mvn/extensions.xml`
activation, matching the current Maven plugin integration harness.

## Goal

Add focused end-to-end Maven plugin integration coverage for strict-mode
rejection of explicitly unmapped lifecycle phases, using a small matrix of
unmapped phase variants instead of a single one-off case.

## Recommendation

Add one new checked-in integration fixture dedicated to strict unmapped phases,
then drive a small matrix from one integration test that invokes Maven against
multiple explicitly unmapped phases.

Recommended phase set:

- `compile`
- `test`
- `package`

This keeps the matrix narrow while still covering:

- a direct compile-phase mapping
- a phase with lifecycle accumulation
- a packaging-oriented lifecycle phase

## Why This Shape

One fixture with several phase invocations is the right trade-off:

- it keeps setup small and readable
- it avoids duplicating fixture files for each phase
- it proves the behavior through the real extension-only activation path
- it preserves the current missing-target strict failure test as separate
coverage for a different failure mode

## Fixture Contract

The new fixture should:

- activate the plugin via `.mvn/extensions.xml`
- include a realistic minimal `pom.xml`
- include a Mill build surface sufficient for the fixture to load normally
- include interceptor config with `strict: true`
- explicitly map selected Maven phases to `unmapped`

The fixture should not rely on missing Mill targets to fail. The failure must
come from the explicit unmapped lifecycle configuration.

## Test Assertions

For each selected Maven phase invocation, the integration test should assert:

- Maven exits non-zero
- output contains `BUILD FAILURE`
- output clearly identifies strict mode as the reason
- output identifies the specific unmapped phase
- output does not depend on a `missing.*` Mill target diagnostic

## Non-Goals

This task should not:

- broaden into YAML vs PKL config matrix coverage
- broaden into module-selection or reactor-shape coverage
- replace the existing missing-target strict failure test
- change lifecycle semantics outside the strict unmapped-phase fixture

## Acceptance

`MI-fuw` is complete when:

- a new integration fixture exists for strict unmapped phases
- the integration spec covers a small phase matrix through that fixture
- the test proves the strict unmapped diagnostic shape end to end
- the existing missing-target strict failure test remains intact

