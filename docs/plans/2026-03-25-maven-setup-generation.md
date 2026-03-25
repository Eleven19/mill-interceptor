# Implementation Plan: CLI-Guided Maven Setup Generation

1. Add CLI result types and parsing for `maven setup` with `--dry-run`,
   `--format`, and `--force`.
2. Add a Maven setup generator service and templates for
   `.mvn/extensions.xml` plus YAML/PKL starter config content.
3. Add repo-root detection and Kyo-backed filesystem write logic.
4. Add CLI and generator tests covering parsing, default format, dry-run,
   overwrite refusal, and forced overwrite.
5. Run formatting and module test suites for `modules/mill-interceptor`.
