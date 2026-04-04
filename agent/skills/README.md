# User-facing agent skills (mill-interceptor)

These skills are **for people using mill-interceptor** in their own projects. They are
published in this repository so users can **copy** them into their agent tooling (for
example Cursor **Rules / Skills**, Claude Code **skills**, or other compatible
layouts) or **reference** them by URL from the GitHub repository.

Maintainer-only automation (changelog, release dispatch, beads worktrees) stays under
`.github/skills/`.

| Skill | Purpose |
|-------|---------|
| [mill-interceptor-setup](mill-interceptor-setup/SKILL.md) | Install CLI, milli launcher, prerequisites |
| [mill-interceptor-maven-extension](mill-interceptor-maven-extension/SKILL.md) | `.mvn/extensions.xml`, baseline lifecycle, generator |
| [mill-interceptor-configuration](mill-interceptor-configuration/SKILL.md) | YAML/Pkl, `mill.environment`, overrides |
| [mill-interceptor-operations](mill-interceptor-operations/SKILL.md) | Daily commands, `inspect-plan`, troubleshooting |

**Documentation site:** https://eleven19.github.io/mill-interceptor/
