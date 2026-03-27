# 0.4.0 RC1 and Documentation Site Design

## Goal

Prepare tracker-visible release work for a `0.4.0-rc.1` prerelease while
separately establishing the documentation-site work that must land before the
final `0.4.0` release.

## Scope

This design covers:

- creating a release-prep issue for `0.4.0-rc.1`
- creating a documentation-site epic for the final `0.4.0` release
- splitting the documentation-site epic into concrete child tasks
- reflecting the release-prep and documentation-site intent in the checked-in
  changelog

This design does not cover:

- implementing the documentation site itself
- cutting or dispatching the `0.4.0-rc.1` release workflow
- promoting `[Unreleased]` into a `0.4.0-rc.1` changelog section yet

## Constraints

- The repository already documents prerelease versions using semantic prerelease
  syntax such as `0.4.0-rc.1`, not uppercase `RC-01`.
- The user does not want documentation-site implementation included in RC1.
- The repository uses `bd` for all issue tracking, so release-prep and
  documentation-site work should be represented there directly.
- The checked-in changelog should remain aligned with notable release and
  documentation process changes.

## Recommended Approach

Use one narrow issue for the immediate prerelease preparation and one separate
epic for the documentation site needed before the final `0.4.0` release.

The prerelease issue should represent the concrete near-term release cut:

- `Prepare 0.4.0-rc.1 release`

The documentation site should be tracked as a dedicated epic with child tasks
that can move independently without overloading the RC1 scope:

- choose the site stack and information architecture
- bootstrap the site and local authoring workflow
- migrate the core user-facing content into the site
- publish the site and wire repository/release references to it

This structure keeps the RC1 work small and explicit while still making the
final-release prerequisite visible, decomposed, and dependency-aware.

## Issue Structure

### Release-prep task

- Type: `task`
- Priority: `1`
- Title: `Prepare 0.4.0-rc.1 release`

This issue should cover prerelease-specific release-note and workflow work only.

### Documentation epic

- Type: `epic`
- Priority: `1`
- Title: `Introduce documentation site for 0.4.0 release`

This issue represents the final-release prerequisite and owns the downstream
child tasks.

### Documentation child tasks

Create child tasks as `discovered-from` dependencies of the epic:

1. Evaluate and choose the documentation site stack and information
   architecture.
2. Bootstrap the documentation site and local authoring workflow.
3. Migrate the core README and usage documentation into the site.
4. Publish the documentation site and update repository/release references.

## Risks and Mitigations

### RC1 scope creep

Risk:
Documentation-site work could leak into the prerelease cut and delay it.

Mitigation:
Keep the site as a separate epic and avoid attaching implementation work to the
RC1 task.

### Under-specified docs-site work

Risk:
A single broad issue would hide decisions around stack choice, migration, and
publishing.

Mitigation:
Create child tasks that reflect those stages directly.

## Result

After this design is applied, the repository will have a tracker-visible path
for the immediate `0.4.0-rc.1` prerelease and a clear decomposition of the
documentation-site work that must land before the final `0.4.0` release.
