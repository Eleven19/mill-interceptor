# GitHub Copilot Instructions

When writing Scala in this repo, prefer Scala filesystem libraries over raw
`java.nio.file` APIs: in module application or library code, use Kyo's file
capabilities first, then `os-lib`; in `mill-build/`, prefer `os-lib`
directly. Only drop to Java NIO at interop or platform boundaries or when the
Scala options are insufficient.

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
redirect path is resolved relative to the worktree root.
