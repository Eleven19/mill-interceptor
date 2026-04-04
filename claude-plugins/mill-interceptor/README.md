# Claude Code plugin: mill-interceptor

This directory is a **Claude Code** plugin that exposes user workflows as **slash
commands**, similar in spirit to plugins like **beads** (`/beads:…`).

## Slash commands

After the plugin is installed, commands are:

| Slash command | Role |
|---------------|------|
| `/mill-interceptor:setup` | Install CLI, milli, prerequisites |
| `/mill-interceptor:maven-extension` | `.mvn/extensions.xml`, baseline lifecycle |
| `/mill-interceptor:configuration` | YAML/Pkl, `mill.environment`, overrides |
| `/mill-interceptor:operations` | CLI `intercept`, Maven, `inspect-plan`, troubleshooting |

Each command under `commands/` maps to one row above (`setup.md` → `:setup`, etc.).

## Canonical skill content

Authoritative prose lives under the repository **`agent/skills/`** directory (paths
referenced from the **mill-interceptor repository root**). The command stubs here
tell the agent to load those files.

## Install (local path)

From the mill-interceptor repository root, add this plugin via Claude Code’s plugin
UI using the path:

`claude-plugins/mill-interceptor`

(Exact menu wording may vary by Claude Code version; choose install from a local
directory pointing at this folder.)

## Copy vs symlink

Users may also copy `claude-plugins/mill-interceptor` into their own project if they
want the same slash commands while developing against a published `mill-interceptor`
version — adjust any paths in `commands/*.md` if skills live elsewhere.
