# Beads Worktrees

This repository uses `bd` with a root `.beads` store. Worktrees that need
tracker access must share that root store instead of trying to host their own
partial beads installation.

## Supported workflow

Create tracker-aware worktrees with:

```bash
bd worktree create .worktrees/<name> --branch <name>
```

That command creates a Git worktree and a `.beads/redirect` file so tracker
commands inside the worktree resolve back to the repository root `.beads`.
For this repository, `.worktrees/` is the preferred location for repo-local
worktrees.

If you create a worktree with plain `git worktree add`, either:

- do not use `bd` inside that worktree, or
- repair it to the redirect-based shape before using tracker commands

## Broken worktree symptoms

A broken worktree usually has:

- a local `.beads` directory
- no `.beads/redirect`
- `bd doctor --json` reporting `No dolt database found`
- `bd ready --json` or similar commands failing with the Dolt circuit breaker

## Recovery

1. Verify whether the worktree is still needed.
2. If it is stale and clean, prune it with `git worktree remove --force`.
3. If it is still needed:
   - back up the local `.beads` stub outside the repo
   - add `.beads/redirect` pointing at the root `.beads`
   - verify with `bd where --json` and `bd ready --json`

Important detail:

- the path in `.beads/redirect` is resolved relative to the worktree root, not
  relative to the `.beads` directory

Example for this repository's preferred `.worktrees/<name>` layout:

```text
../../.beads
```

## Verification

From the repaired worktree:

```bash
bd where --json
bd ready --json
```

From the root repo:

```bash
bd doctor --json
bd dolt push --json
```
