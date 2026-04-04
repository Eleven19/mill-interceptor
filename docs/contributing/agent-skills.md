# User-facing agent skills and Claude Code plugin

End users of **mill-interceptor** (not maintainers of this repository) can adopt
curated agent instructions and Claude Code slash commands without copying
maintainer automation from `.github/skills/`.

## Where things live

| Location | Purpose |
|----------|---------|
| `agent/skills/` | Copyable **SKILL.md** files: setup, Maven extension, configuration, operations. See `agent/skills/README.md`. |
| `claude-plugins/mill-interceptor/` | **Claude Code** plugin: `commands/*.md` map to slash commands that tell the agent to load the matching skill. See that folder’s `README.md`. |

## Slash commands (Claude Code)

After installing the local plugin from `claude-plugins/mill-interceptor`:

| Command | Topic |
|---------|--------|
| `/mill-interceptor:setup` | Install CLI, milli, prerequisites |
| `/mill-interceptor:maven-extension` | `.mvn/extensions.xml`, baseline lifecycle |
| `/mill-interceptor:configuration` | YAML/Pkl, `mill.environment`, overrides |
| `/mill-interceptor:operations` | CLI `intercept`, Maven, troubleshooting |

This mirrors the pattern used by plugins such as **beads** (`/beads:…`).

## Published documentation

The documentation site remains the long-form source of truth:

- https://eleven19.github.io/mill-interceptor/

## Maintainer skills

Changelog, release automation, and beads worktree repair stay under:

- `.github/skills/`
