# Namespace And Group Rename Design

## Summary

Rename the canonical Scala package base from `io.github.eleven19.mill.interceptor` to
`io.eleven19.mill.interceptor`, and rename the Maven Central group from
`io.github.eleven19.mill-interceptor` to `io.eleven19.mill-interceptor`.

This is a full namespace migration rather than a publish-only tweak. The repository
layout, build metadata, runtime identifiers, tests, and active user-facing docs should
all reflect the new canonical names.

## Goals

- make `io.eleven19.mill.interceptor` the only active Scala package base
- make `io.eleven19.mill-interceptor` the Maven publishing group
- keep source tree layout aligned with package layout across `src/`, `test/`, and `itest/`
- update user-facing docs and metadata so published usage examples match the new names
- preserve historical plan documents while leaving a note that explains the rename

## Non-Goals

- rewriting historical design documents line-by-line
- changing artifact IDs such as `milli`, `milli-dist`, or `milli-native-*`
- adding compatibility shim packages under the old namespace

## Approach Options

### 1. Full rename with targeted doc updates

Rename packages, directories, build metadata, runtime strings, and active docs. Leave
historical plan docs untouched except for a note in `docs/plans/`.

Pros:

- keeps the repository internally consistent
- minimizes long-term confusion
- avoids temporary compatibility layers

Cons:

- touches many files at once

### 2. Hybrid rename with compatibility wrappers

Add forwarding packages or duplicate entry points under the old namespace while moving
the canonical code to the new one.

Pros:

- softer migration path for any hidden downstream internal consumers

Cons:

- adds noise and maintenance burden
- unnecessary unless we know the old package is consumed externally

### 3. Coordinates/docs first, code later

Change publishing and docs now, then defer the package rename.

Pros:

- smaller short-term change

Cons:

- leaves the repo inconsistent
- increases confusion during follow-up work

## Recommended Approach

Use option 1: perform the full namespace migration in code and repository layout, update
active docs and metadata, and leave historical plans intact with a note explaining the
rename.

## Architecture

### Canonical Names

- Scala package base: `io.eleven19.mill.interceptor`
- Maven group: `io.eleven19.mill-interceptor`

### Code And Filesystem Scope

Move these trees to match the new package base:

- `src/io/github/eleven19/...` -> `src/io/eleven19/...`
- `test/io/github/eleven19/...` -> `test/io/eleven19/...`
- `itest/io/github/eleven19/...` -> `itest/io/eleven19/...`

Then update all package declarations and imports to the new namespace.

### Build And Publish Metadata

Update:

- `build.mill.yaml` `mainClass`
- publish support Maven group constant
- publish metadata tests
- README Maven Central badge and example coordinates

Artifact IDs stay unchanged.

### Runtime Identifiers

Update logger/category strings, comments, and any other emitted identifiers that still
carry `io.github.eleven19...`.

### Documentation

Update active docs such as:

- `README.md`
- release/contributing docs
- any test or config glue that references package names directly

Do not rewrite historical plan documents. Instead, add a short note under `docs/plans/`
explaining that older plans may refer to the pre-rename namespace and Maven group.

## Verification

- `rg` should show no active `io.github.eleven19...` references outside preserved
  historical plans and the new explanatory note
- Mill test suite passes
- publish metadata checks pass
- release workflow regression checks pass
- active docs point at `io.eleven19.mill-interceptor`

## Risks

- package and directory renames are mechanically broad, so the change should avoid
  opportunistic refactors
- test/config glue may hide hard-coded package names
- Central signing secret failures remain an infrastructure issue and are out of scope
  for this rename
