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
