# Agent Instructions

This project uses **bd** (beads) for issue tracking. Run `bd onboard` to get started.

## Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work atomically
bd close <id>         # Complete work
bd dolt push          # Push beads data to remote
bd worktree create .worktrees/<name> --branch <name>  # Create a repo-local worktree that shares the root beads store
```

## Non-Interactive Shell Commands

**ALWAYS use non-interactive flags** with file operations to avoid hanging on confirmation prompts.

Shell commands like `cp`, `mv`, and `rm` may be aliased to include `-i` (interactive) mode on some systems, causing the agent to hang indefinitely waiting for y/n input.

**Use these forms instead:**
```bash
# Force overwrite without prompting
cp -f source dest           # NOT: cp source dest
mv -f source dest           # NOT: mv source dest
rm -f file                  # NOT: rm file

# For recursive operations
rm -rf directory            # NOT: rm -r directory
cp -rf source dest          # NOT: cp -r source dest
```

**Other commands that may prompt:**
- `scp` - use `-o BatchMode=yes` for non-interactive
- `ssh` - use `-o BatchMode=yes` to fail instead of prompting
- `apt-get` - use `-y` flag
- `brew` - use `HOMEBREW_NO_AUTO_UPDATE=1` env var

## Scala Filesystem Preference

When writing Scala in this repo, prefer Scala filesystem libraries over raw
`java.nio.file` APIs.

- In application and library code under `modules/`, prefer Kyo's file
  capabilities first so filesystem work stays aligned with the project's effect
  model and test style.
- In `mill-build/`, prefer `os-lib` directly since that is the native build
  runtime abstraction.
- If Kyo is not sufficient in module code or would add unnecessary friction,
  prefer `os-lib` next.
- Use `java.nio.file` only at interop or platform boundaries, or when Kyo and
  `os-lib` cannot reasonably express the need.

## Shared Release Skills

For changelog and release tasks, use the repo-local skills and supporting docs
instead of inventing a parallel process:

- `.github/skills/changelog-maintenance/SKILL.md`
- `.github/skills/release-process/SKILL.md`
- `docs/contributing/changelog.md`
- `docs/contributing/releasing.md`

## User-facing agent skills (mill-interceptor)

Skills for **end users** adopting mill-interceptor (install, Maven extension, config,
operations) live under **`agent/skills/`**. Users can copy them into their own agent
tooling or read them from this repository; they are not maintainer automation.

- `agent/skills/README.md` (index)
- `agent/skills/mill-interceptor-setup/SKILL.md`
- `agent/skills/mill-interceptor-maven-extension/SKILL.md`
- `agent/skills/mill-interceptor-configuration/SKILL.md`
- `agent/skills/mill-interceptor-operations/SKILL.md`

<!-- BEGIN BEADS INTEGRATION -->
## Issue Tracking with bd (beads)

**IMPORTANT**: This project uses **bd (beads)** for ALL issue tracking. Do NOT use markdown TODOs, task lists, or other tracking methods.

### Why bd?

- Dependency-aware: Track blockers and relationships between issues
- Git-friendly: Dolt-powered version control with native sync
- Agent-optimized: JSON output, ready work detection, discovered-from links
- Prevents duplicate tracking systems and confusion

### Quick Start

**Check for ready work:**

```bash
bd ready --json
```

**Create new issues:**

```bash
bd create "Issue title" --description="Detailed context" -t bug|feature|task -p 0-4 --json
bd create "Issue title" --description="What this issue is about" -p 1 --deps discovered-from:bd-123 --json
```

**Claim and update:**

```bash
bd update <id> --claim --json
bd update bd-42 --priority 1 --json
```

**Complete work:**

```bash
bd close bd-42 --reason "Completed" --json
```

### Issue Types

- `bug` - Something broken
- `feature` - New functionality
- `task` - Work item (tests, docs, refactoring)
- `epic` - Large feature with subtasks
- `chore` - Maintenance (dependencies, tooling)

### Priorities

- `0` - Critical (security, data loss, broken builds)
- `1` - High (major features, important bugs)
- `2` - Medium (default, nice-to-have)
- `3` - Low (polish, optimization)
- `4` - Backlog (future ideas)

### Workflow for AI Agents

1. **Check ready work**: `bd ready` shows unblocked issues
2. **Claim your task atomically**: `bd update <id> --claim`
3. **Work on it**: Implement, test, document
4. **Discover new work?** Create linked issue:
   - `bd create "Found bug" --description="Details about what was found" -p 1 --deps discovered-from:<parent-id>`
5. **Complete**: `bd close <id> --reason "Done"`

### Auto-Sync

bd automatically syncs via Dolt:

- Each write auto-commits to Dolt history
- Use `bd dolt push`/`bd dolt pull` for remote sync
- No manual export/import needed!

### Worktrees

When a task needs tracker access from a Git worktree, create the worktree with
`bd worktree create`, not plain `git worktree add`. The beads-aware command
creates `.beads/redirect` so the worktree shares the root repository's tracker
store.

Rules:

- Prefer placing repo-local worktrees under `.worktrees/`, for example
  `bd worktree create .worktrees/<name> --branch <name>`.
- Run tracker commands from the repo root or from worktrees created with
  `bd worktree create`.
- If a worktree has `.beads` but no `.beads/redirect`, treat it as
  misconfigured.
- The redirect path in `.beads/redirect` is resolved relative to the worktree
  root, not relative to the `.beads` directory.
- Use the repo-local skill `.github/skills/beads-worktree-recovery/SKILL.md`
  and `docs/contributing/beads-worktrees.md` when diagnosing or repairing this
  state.

### Important Rules

- ✅ Use bd for ALL task tracking
- ✅ Always use `--json` flag for programmatic use
- ✅ Link discovered work with `discovered-from` dependencies
- ✅ Check `bd ready` before asking "what should I work on?"
- ❌ Do NOT create markdown TODO lists
- ❌ Do NOT use external issue trackers
- ❌ Do NOT duplicate tracking systems

For more details, see README.md and docs/QUICKSTART.md.

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

<!-- END BEADS INTEGRATION -->
