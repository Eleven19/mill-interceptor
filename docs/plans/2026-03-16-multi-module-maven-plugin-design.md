# Multi-Module Maven Plugin Design

## Summary

Restructure the repository from a single root product into a declarative-first
multi-module Mill build. The root remains the aggregate entrypoint in
`build.mill.yaml`, shared programmatic build logic stays in `mill-build/`, the
current CLI/interceptor product moves under `modules/mill-interceptor`, and a
new sibling module `modules/mill-interceptor-maven-plugin` is added for future
Maven plugin work.

This first pass is structural only. It should create the module boundaries and
build wiring needed for later feature work without introducing lifecycle
interception, YAML mapping, or PKL support yet.

## Goals

- make the repository a real multi-module Mill build rooted at `build.mill.yaml`
- keep `mill-build/` as the shared programmatic build layer for declarative YAML
- move the current product code and tests into `modules/mill-interceptor`
- add a minimal `modules/mill-interceptor-maven-plugin` skeleton
- preserve the existing `milli` artifact ownership under the moved
  `mill-interceptor` module
- keep current CLI behavior and test coverage intact after the move

## Non-Goals

- implementing Maven lifecycle interception
- designing the YAML or PKL mapping format
- wiring Maven goals/phases to Mill tasks
- publishing the Maven plugin artifact unless the structural wiring requires a
  placeholder publish shape
- changing the current runtime behavior of the interceptor CLI

## Approach Options

### 1. Real aggregate root with product modules

Turn the repository root into a non-publishable aggregate/build module. Move the
current CLI into `modules/mill-interceptor` and add
`modules/mill-interceptor-maven-plugin` as a second product module.

Pros:

- matches the desired repository layout
- keeps ownership of publishable products explicit
- scales cleanly as more modules appear
- avoids root-module special cases later

Cons:

- requires build rewiring and path migration up front

### 2. Keep the root publishable and move only the folders

Leave the root as the current product module but point its source/test roots at
`modules/mill-interceptor/...`.

Pros:

- smaller short-term build diff

Cons:

- the filesystem and module graph disagree
- leaves root-specific assumptions in place
- complicates later plugin work

### 3. Add a sibling plugin module without a `modules/` directory

Keep the existing root module unchanged and bolt on a new plugin module next to
it.

Pros:

- smallest immediate change

Cons:

- does not satisfy the requested repository layout
- becomes awkward if more products are added

## Recommended Approach

Use option 1. Keep the root declarative-first and aggregate-only, preserve
shared code in `mill-build/`, and move product ownership into explicit module
directories under `modules/`.

## Architecture

### Build Entry Points

- `build.mill.yaml` remains the top-level build definition and declares the
  repository modules
- `mill-build/src/build` remains the place for shared programmatic traits and
  reusable module definitions used by the YAML build

### Module Layout

- `modules/mill-interceptor`
  - owns the current interceptor CLI/library
  - owns the existing source, unit-test, and integration-test trees
  - remains the owner of `milli`, `milli-dist`, and the current native release
    artifacts
- `modules/mill-interceptor-maven-plugin`
  - starts as a minimal Scala module skeleton
  - is structurally ready for later Maven Mojo/lifecycle work
  - should compile and have at least one placeholder test in the first pass

### Root Responsibilities

The root should coordinate the build and shared settings only. It should not be
the publishable owner of the current product after the restructure.

### Naming

Use `mill-interceptor-maven-plugin` for the new module and eventual artifact
base name. That matches Maven plugin naming conventions more closely than
`mill-interceptor-maven` and makes the purpose explicit.

## Migration Plan

### Filesystem Move

Move these trees:

- `src/` -> `modules/mill-interceptor/src/`
- `test/` -> `modules/mill-interceptor/test/`
- `itest/` -> `modules/mill-interceptor/itest/`

Any module-local Mill YAML files should move with the product module rather than
staying at the repository root.

### Build Rewiring

Update the declarative build so that:

- the root declares and exposes both modules
- shared traits from `mill-build/` can be reused by both modules
- the moved `mill-interceptor` module preserves the current main class, native
  image settings, and publish/release behavior
- the new Maven plugin module is present in the build graph with minimal compile
  and test wiring

### Compatibility

Existing developer workflows for the current product should continue to work,
even if root aliases are needed to smooth over task-path changes. Structural
refactoring should not force behavior changes into the same patch.

## Testing And Verification

The first pass is successful when:

- the moved `mill-interceptor` module compiles
- the existing unit tests still pass from the new module location
- the existing integration tests still pass from the new module location
- publish/release metadata for the current product still resolves from
  `modules/mill-interceptor`
- the new Maven plugin module is recognized by Mill and compiles with its
  placeholder code

## Risks

- declarative Mill YAML rewiring can hide path mistakes until task resolution or
  test execution
- release/publish support currently assumes a root product and may need shared
  base-module extraction in `mill-build/`
- moving `itest/` may require careful updates to any relative resource or task
  assumptions
- adding a plugin skeleton without overcommitting to the final plugin API
  requires discipline in naming and placeholder scope
