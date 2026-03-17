---
name: beads-worktree-recovery
description: Use when bd fails inside a Git worktree with missing database, circuit-breaker, or redirect-related errors.
---

# Beads Worktree Recovery

Use this when a worktree has tracker problems that do not reproduce from the
repository root.

## Failure pattern

Look for this combination:

- the root repo `bd` commands work
- the worktree has a local `.beads` directory
- the worktree lacks `.beads/redirect`
- `bd doctor --json` reports `No dolt database found`
- `bd ready --json` fails with Dolt server or circuit-breaker errors

## Root cause

The worktree was created without beads' redirect-based setup. `bd worktree
create` creates a `.beads/redirect` file so the worktree shares the root
repository tracker store. A local `.beads` stub without a redirect is a broken
state.

## Repair

1. Check whether the worktree is still needed.
2. If it is stale and clean, prune it with `git worktree remove --force`.
3. If it is still needed:
   - back up the worktree-local `.beads` directory outside the repo
   - add `.beads/redirect` that points at the root repo `.beads`
   - verify from the worktree with `bd where --json` and `bd ready --json`

Important:

- the redirect path is resolved relative to the worktree root, not relative to
  the `.beads` directory

## Verification

From the worktree:

```bash
bd where --json
bd ready --json
```

From the root repo:

```bash
bd doctor --json
bd dolt push --json
```

## References

- `docs/contributing/beads-worktrees.md`
- `AGENTS.md`
