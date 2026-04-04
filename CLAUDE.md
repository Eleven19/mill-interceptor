# Claude Instructions

When writing Scala in this repo, prefer `os-lib` for filesystem operations
over raw `java.nio.file` APIs. In `mill-build/`, use `os-lib` directly.
Only drop to Java NIO at interop or platform boundaries.

## Direct-Style Architecture (Ox + PureLogic)

This codebase uses direct-style Scala 3:
- **Ox** for concurrency and application entry points (`OxApp`)
- **PureLogic** for typed errors in pure domain logic via context functions
- **os-lib** for filesystem operations
- **Scribe** for logging (direct calls, no effect wrapper)
- **ZIO Test** for unit and integration tests

Do NOT use: Kyo, cats-effect, ZIO effects, Futures, or monadic style.
Use plain Scala control flow. Blocking I/O is fine (virtual threads).
Use `Either[E, A]` for typed errors at I/O boundaries.
Use PureLogic `Abort[E]` for typed errors in pure domain functions.

For changelog or release-related work, consult these repo-local skills first:

- `.github/skills/changelog-maintenance/SKILL.md`
- `.github/skills/release-process/SKILL.md`

Use the supporting repo docs instead of duplicating process text:

- `docs/contributing/changelog.md`
- `docs/contributing/releasing.md`

**User-facing skills** (for people using mill-interceptor in their projects, not
maintainers): see `agent/skills/README.md` and the `mill-interceptor-*` skill files
under `agent/skills/`.

**Claude Code slash commands:** install `claude-plugins/mill-interceptor`, then use
`/mill-interceptor:setup`, `/mill-interceptor:maven-extension`,
`/mill-interceptor:configuration`, `/mill-interceptor:operations` (see
`claude-plugins/mill-interceptor/README.md`).

For beads tracker work in this repo, run commands from the root checkout or
from worktrees created with `bd worktree create`. If a worktree has `.beads`
but no `.beads/redirect`, it is misconfigured; use
`.github/skills/beads-worktree-recovery/SKILL.md` and
`docs/contributing/beads-worktrees.md` to repair it. The redirect path is
resolved relative to the worktree root. Prefer repo-local worktrees under
`.worktrees/`, for example
`bd worktree create .worktrees/<name> --branch <name>`.
