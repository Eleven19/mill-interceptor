# Publish API Cleanup Design

Date: 2026-03-17
Issue: `MI-dyl`

## Problem

Mill `1.1.3` deprecates the legacy `publishArtifacts` task shape. This
repository still relies on that deprecated API in two places:

- [mill-build/src/build/Modules.scala](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-dyl-publish-api-cleanup/mill-build/src/build/Modules.scala)
  for Maven plugin jar and pom lookup
- [mill-build/src/build/PublishSupport.scala](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-dyl-publish-api-cleanup/mill-build/src/build/PublishSupport.scala)
  for the assembly and native publish modules

The current behavior works, but it leaves the build on deprecated extension
points and risks future breakage.

## Goal

Move the repo's custom publish logic onto Mill's current payload-oriented publish
API without changing published coordinates, filenames, or release behavior.

## Scope

In scope:

- replace deprecated `publishArtifacts` read usage in `MavenPluginSupport`
- replace deprecated `publishArtifacts` overrides in `PublishSupport`
- preserve current artifact naming and publish surfaces
- verify that the Maven plugin integration tests and publish checks still pass

Out of scope:

- changing artifact IDs, coordinates, or release asset names
- broader refactors of the release architecture
- contributor docs beyond design/plan unless needed for clarity

## Chosen Approach

Use a staged repo-wide cleanup.

1. Update `MavenPluginSupport` to read from the current publish payload API
   instead of calling `publishArtifacts()`.
2. Update the assembly and native publish modules in `PublishSupport` to use the
   replacement payload hook instead of overriding deprecated `publishArtifacts`.
3. Keep the current public interface intact:
   - `publishSonatypeCentral` remains the entrypoint
   - existing artifact metadata stays the same
   - existing tests still resolve the same jar, pom, and native payloads

## Risks

- The assembly/native publish modules synthesize artifacts outside the default
  Scala jar flow, so the replacement hook may differ from the simple Maven
  plugin lookup case.
- The deprecated API may still appear elsewhere after the first change.

## Risk Mitigation

- verify the Maven plugin path first
- verify the assembly/native path second
- run the existing publish metadata and publish workflow checks after the code
  change
- treat any remaining `publishArtifacts` deprecation warning as unfinished work

## Success Criteria

- the deprecated `publishArtifacts` warning is gone from the touched build
  codepath
- Maven plugin jar and pom lookup still work
- assembly/native publish metadata remains unchanged
- existing publish verification scripts still pass
