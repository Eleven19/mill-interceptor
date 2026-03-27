# Contributing

Notes and short articles for anyone working on this codebase (including future you). They explain design choices, integrations, and how to change or extend things.

## Coding Conventions

For Scala filesystem work, prefer Scala libraries over raw `java.nio.file`
types. In application and library code under `modules/`, use Kyo's file
capabilities first so filesystem code composes with the repo's effect model and
testing style. In `mill-build/`, prefer `os-lib` directly because it is the
natural build-runtime abstraction. If Kyo is not sufficient in module code or
would add unnecessary friction, prefer `os-lib` next. Use Java NIO only at
interop or platform boundaries, or when neither Scala option can reasonably
express the need.

## Articles

- **[Beads Worktrees](beads-worktrees.md)** — How to create tracker-aware worktrees, how to recognize a broken local `.beads` stub, and how to repair a worktree back to the root beads store.
- **[Maintaining the Changelog](changelog.md)** — How `CHANGELOG.md` is structured, how `[Unreleased]` is maintained, and what has to exist before a release can be published.
- **[Logging with Scribe](logging-with-scribe.md)** — How Kyo’s logging works and how we switched this app to use Scribe as the backend (implementing `Log.Unsafe`, wiring with `Log.let`).
- **[Releasing Native Archives](releasing.md)** — How the GitHub release workflow builds native archives, how versioning works, and how the assets are named for `mise`.

## Documentation Site

User-facing docs are published at:

- <https://eleven19.github.io/mill-interceptor/>

Local generation and preview:

- `./mill modules.mill-interceptor.docJar`
- `./mill modules.mill-interceptor.docSiteServe`
