# Copilot Coding Agent — Firewall Allowlist

The Copilot coding agent runs in a sandboxed environment with a firewall that
blocks outbound network access by default. The following domains must be added
to the **custom allowlist** in this repository's
[Copilot coding agent settings](https://github.com/Eleven19/mill-interceptor/settings/copilot/coding_agent)
(admins only).

## Required domains

| Domain | Purpose |
|---|---|
| `ox.softwaremill.com` | Ox documentation — direct-style concurrency library |
| `ghostdogpr.github.io` | PureLogic documentation — typed errors via context functions |
| `virtuslab.com` | VirtusLab blog — direct-style Scala 3 patterns and guidance |
| `context7.com` | Context7 — up-to-date library documentation index |

## Why these are needed

This codebase uses Ox and PureLogic as core architectural libraries. When
Copilot reviews or modifies code, it needs access to these docs to:

- Verify correct API usage (e.g., `Abort { ... }` vs `Abort[E]: ...`)
- Reference error handling patterns (`either:` blocks, `Abort` context functions)
- Check Ox concurrency patterns (`supervised`, `fork`, `par`)
- Validate direct-style idioms against official guidance

Without access, Copilot may generate incorrect API calls (as happened in PR #30
where `Abort[E]:` was used instead of `Abort { ... }` because the PureLogic
docs were blocked).

## Alternative: setup steps

If the allowlist approach is not preferred, a `.github/copilot-setup-steps.yml`
can pre-fetch documentation before the firewall activates. See
[GitHub docs on Actions setup steps](https://gh.io/copilot/actions-setup-steps).
