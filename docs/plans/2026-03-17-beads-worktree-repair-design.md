# Beads Worktree Repair Design

Date: 2026-03-17
Issue: `MI-7y2`

## Problem

This repository has a healthy root beads installation in [`.beads`](/home/damian/code/repos/github/Eleven19/mill-interceptor/.worktrees/mi-7y2-beads-worktree-repair/.beads), but some ad hoc Git worktrees also contain their own partial `.beads` directories. Those worktree-local directories have metadata and hooks but no Dolt database and no `.beads/redirect`.

When `bd` runs inside one of those worktrees, it treats the worktree as a separate broken beads installation. That produces failures such as:

- `No dolt database found`
- `failed to open database: dolt circuit breaker is open: server appears down`

The result is repeated manual recovery work, confusion about whether the root tracker is corrupted, and fragile future sessions.

## Root Cause

The failing worktrees were not created with beads' supported worktree flow. `bd worktree create` creates a `.beads/redirect` file so all worktrees share the root repository's beads store. The broken worktrees have a local `.beads` stub instead of a redirect, so tracker commands in those directories do not resolve back to the root store.

## Goals

- Remove stale worktrees that are no longer needed.
- Repair still-relevant worktrees so beads resolves to the root store.
- Document the supported worktree pattern for this repository.
- Add reusable recovery guidance so future agents can diagnose and fix the same problem quickly.
- Verify the repaired state from both the repository root and a redirected worktree.

## Non-Goals

- Changing beads upstream behavior.
- Adding repo-local automation that overrides beads' own worktree management.
- Reworking general Dolt recovery beyond what is required for this worktree misconfiguration.

## Chosen Approach

Use beads' native redirect model rather than custom workarounds.

1. Prune stale worktrees that are clean and no longer in use.
2. Repair still-active worktrees in place by replacing the broken local `.beads` stub with the supported redirect shape.
3. Add project guidance that beads operations should run from the root repo or from worktrees created via `bd worktree create`.
4. Add both:
   - a repo-local guidance document tied to this repository
   - a personal Codex skill for reusable diagnosis and recovery

## Validation

The repaired state should satisfy all of the following:

- `bd where --json` from the root repo points at the root `.beads`.
- `bd where --json` from a repaired worktree reports `redirected_from` and resolves to the root `.beads`.
- `bd doctor --json` succeeds from the root repo.
- `bd ready --json` works from the root repo and from at least one repaired worktree.
- `bd dolt push --json` completes from the root repo.

## Risks

- Removing a worktree that the user still needs.
  - Mitigation: only prune worktrees that are clean, stale, and have no active upstream.
- Overwriting useful worktree-local beads state.
  - Mitigation: the broken worktrees do not contain a local Dolt database; back up the old `.beads` stub before replacing it.
- Creating documentation without a reusable entry point.
  - Mitigation: pair repo guidance with a personal skill and verify both are discoverable.
