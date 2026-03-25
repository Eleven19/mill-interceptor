# CLI-Guided Maven Setup Generation Design

## Context

The repository now has a working Maven core-extension path that can be activated
with `.mvn/extensions.xml`, and the published docs describe a minimal setup plus
optional `mill-interceptor.yaml|pkl` overrides. The CLI currently supports
`intercept` and `shim generate`, but it has no repo-setup command to scaffold the
Maven extension configuration for users.

## Goals

- add a first-class CLI command for Maven setup generation
- write setup files into the repository by default
- support `--dry-run` for previewing writes without mutation
- generate `.mvn/extensions.xml`
- generate a starter config file by default, with YAML as the default format
- keep the generated config valid and useful as-is while including commented
  hints for common overrides
- fail safely when target files already exist unless `--force` is provided

## Non-Goals

- probing the Mill build and inferring exact project mappings
- end-to-end Maven execution verification from the CLI task itself
- making PKL the default starter format
- adding interactive prompts in the first version

## Command Shape

Add a new CLI command:

- `mill-interceptor maven setup`

Default behavior:
- write `.mvn/extensions.xml`
- write `mill-interceptor.yaml`
- create parent directories as needed
- print the files written and short next steps

Flags:
- `--dry-run` to print planned writes without mutation
- `--format yaml|pkl` with `yaml` as the default
- `--force` to overwrite existing generated files

## File Behavior

The command should resolve output paths relative to the repo root, regardless of
where inside the repo the command is invoked.

Writes:
- `.mvn/extensions.xml`
- `mill-interceptor.yaml` by default
- `mill-interceptor.pkl` when `--format pkl` is selected

Overwrite policy:
- fail by default if a target file already exists
- allow overwrite only with `--force`

Dry-run policy:
- print the files that would be written
- print rendered content
- write nothing
- still report blocking existing files when a real run would fail

## Generated Content

`.mvn/extensions.xml` should contain the published extension coordinates for
`io.eleven19.mill-interceptor:mill-interceptor-maven-plugin`.

The starter config should:
- be valid and usable as-is
- align with the conventional baseline
- include commented examples for:
  - disabling the validate Scalafmt hook
  - overriding a lifecycle phase mapping
  - module-local override discovery hints
  - the requirement for a `PublishModule` surface when using Maven `install` and
    `deploy`

## Implementation Approach

Add:
- CLI parsing updates for `maven setup`
- a small generator service in the CLI module
- renderers for `extensions.xml` and starter config content
- Kyo-first file writes and repo-root detection

## Testing

- CLI parsing tests for `maven setup`, `--dry-run`, `--format`, and `--force`
- generator tests for rendered file contents
- filesystem tests for writing files, overwrite refusal, `--force`, and dry-run
- an assertion that YAML is the default format
- a small content check for the generated extension coordinates
