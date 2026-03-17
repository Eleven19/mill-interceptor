# Claude Instructions

When writing Scala in this repo, prefer Scala filesystem libraries over raw
`java.nio.file` APIs: in module application or library code, use Kyo's file
capabilities first, then `os-lib`; in `mill-build/`, prefer `os-lib`
directly. Only drop to Java NIO at interop or platform boundaries or when the
Scala options are insufficient.

For changelog or release-related work, consult these repo-local skills first:

- `.github/skills/changelog-maintenance/SKILL.md`
- `.github/skills/release-process/SKILL.md`

Use the supporting repo docs instead of duplicating process text:

- `docs/contributing/changelog.md`
- `docs/contributing/releasing.md`

For beads tracker work in this repo, run commands from the root checkout or
from worktrees created with `bd worktree create`. If a worktree has `.beads`
but no `.beads/redirect`, it is misconfigured; use
`.github/skills/beads-worktree-recovery/SKILL.md` and
`docs/contributing/beads-worktrees.md` to repair it. The redirect path is
resolved relative to the worktree root.
