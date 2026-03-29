# GitHub Copilot Instructions

## Direct-Style Architecture (Ox + PureLogic)

This codebase uses direct-style Scala 3. Do NOT use Kyo, cats-effect, ZIO
effects, Futures, or monadic style (no nested flatMap/map chains).

- **Ox** for concurrency and application entry points (`OxApp`)
- **PureLogic** for typed errors in pure domain logic via context functions
- **os-lib** for filesystem operations (prefer over raw `java.nio.file` APIs)
- **Scribe** for logging (direct calls, no effect wrapper)
- **ZIO Test** for unit and integration tests

Use plain Scala control flow. Blocking I/O is fine (virtual threads).
Use `Either[E, A]` for typed errors at I/O boundaries.
Use PureLogic `Abort[E]` via context functions for typed errors in pure
domain functions — see https://ghostdogpr.github.io/purelogic/abort.html

For Ox patterns and error handling, reference:
- https://ox.softwaremill.com/latest/basics/error-handling.html
- https://ox.softwaremill.com/latest/info/ai.html

In `mill-build/`, prefer `os-lib` directly. Only drop to Java NIO at interop
or platform boundaries.

For changelog or release tasks, prefer the repo-local skills in `.github/skills/`
instead of inventing a separate process:

- `.github/skills/changelog-maintenance/SKILL.md`
- `.github/skills/release-process/SKILL.md`

Use `CHANGELOG.md`, `docs/contributing/changelog.md`,
`docs/contributing/releasing.md`, `cliff.toml`, and
`scripts/ci/build-release-notes.sh` as the source of truth.

For beads tracker work, prefer the root repository checkout or worktrees
created with `bd worktree create`. If a worktree has `.beads` but no
`.beads/redirect`, it is misconfigured and should be repaired with the
repo-local skill `.github/skills/beads-worktree-recovery/SKILL.md`. The
redirect path is resolved relative to the worktree root. Prefer repo-local
worktrees under `.worktrees/`, for example
`bd worktree create .worktrees/<name> --branch <name>`.
