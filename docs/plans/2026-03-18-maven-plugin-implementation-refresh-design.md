# Maven Plugin Implementation Refresh Design

## Summary

Implement the real `mill-interceptor-maven-plugin` so Maven becomes a
predictable entrypoint into Mill for the full common lifecycle:

- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `install`
- `deploy`

The plugin should remain strict-first in execution semantics, but config
resolution should start from a documented conventional lifecycle baseline so the
common case requires little or no user configuration. YAML and PKL remain the
customization surface, with PKL acting as the canonical composition layer.

This refresh updates the earlier design in two key ways:

- full lifecycle coverage is the first milestone
- strict mode includes a conventional baseline seed rather than requiring every
  lifecycle mapping to be restated explicitly

## Goals

- make Maven a real entrypoint into Mill across the full common lifecycle
- keep behavior deterministic and testable
- minimize required configuration for common Mill projects
- preserve strong override power through YAML and PKL
- keep external execution through the `mill` CLI
- implement effectful boundaries with Kyo and logging with scribe
- provide useful diagnostics when the conventional baseline is insufficient

## Non-Goals

- transparent interception of arbitrary third-party Maven plugin ecosystems in
  the first implementation
- introducing a separate user-facing conventional mode in the first
  implementation
- in-process Mill embedding
- heuristic behavior that cannot be explained or tested deterministically
- reactor-wide optimization beyond reliable per-module execution

## Recommended Approach

Use lifecycle-bound Mojos for the full common lifecycle plus a small set of
operational goals such as `describe` and `inspect-plan`. Resolve Maven
execution through layered config and an internal conventional lifecycle
baseline, then translate the final result into a `MillExecutionPlan` executed
through the external `mill` CLI.

This preserves the design intent:

- strict mode still governs failure semantics
- users still get deterministic, explicit override behavior
- minimal configuration stays small because common lifecycle behavior starts
  from a conventional seed

## Architecture

The plugin should remain layered:

- Maven boundary layer
  - lifecycle-bound Mojos
  - operational goals like `describe` and `inspect-plan`
- config layer
  - file discovery
  - YAML parsing
  - PKL evaluation
  - validation
- resolution layer
  - merges the conventional lifecycle baseline with user overrides
  - produces a `MillExecutionPlan`
- execution layer
  - Kyo-native process execution for the external `mill` CLI
- diagnostics layer
  - strict failures
  - missing-target guidance
  - plan inspection output

The plugin module remains:

- `modules/mill-interceptor-maven-plugin`

The published coordinate remains:

- `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin:<version>`

## Runtime Model

### Strict-First Behavior

The first implementation should remain strict-first:

- supported lifecycle phases are resolved through the baseline plus config
- explicitly configured goals are resolved through the same system
- unsupported or explicitly unmapped execution fails clearly

This is not a separate conventional mode. It is strict mode with a documented
default mapping seed.

### Conventional Lifecycle Baseline

The resolver should begin from a conventional lifecycle mapping for common
Mill-based projects. Recommended baseline:

- `clean`
  - Mill clean-equivalent behavior
- `validate`
  - no-op by default, with optional checks such as Scalafmt verification
- `compile`
  - compile target
- `test`
  - compile plus test targets
- `package`
  - compile, test, and package-equivalent target
- `verify`
  - package-equivalent behavior plus configured verification hooks
- `install`
  - local publish-equivalent behavior
- `deploy`
  - remote publish-equivalent behavior

The baseline should be overrideable at repo and module scope through YAML and
PKL so users can replace any phase mapping without restating the entire
lifecycle.

## Configuration Model

### Supported Files

Primary names:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`

Alternate config-directory names:

- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

### Discovery Order

The approved order remains:

1. repository `mill-interceptor.yaml`
2. repository `mill-interceptor.pkl`
3. repository `.config/mill-interceptor/config.yaml`
4. repository `.config/mill-interceptor/config.pkl`
5. module `mill-interceptor.yaml`
6. module `mill-interceptor.pkl`
7. module `.config/mill-interceptor/config.yaml`
8. module `.config/mill-interceptor/config.pkl`

### Composition Model

YAML is a data input format. PKL is the canonical composition layer.

The plugin should:

- parse YAML into canonical config inputs
- expose those values to PKL
- let PKL perform amend and override behavior
- validate only the final effective config

### Minimal Useful Config

The smallest useful config should ideally only be needed when a repo differs
from the conventional baseline. Typical reasons:

- the `mill` executable is not on `PATH`
- working-directory behavior must be customized
- packaging or publish behavior differs from the baseline
- extra goals or verification hooks are desired

That means a standard Mill build should be able to get most of the common Maven
lifecycle behavior with zero or near-zero config.

## Scalafmt Validation Hook

Scalafmt verification should be modeled as an optional `validate`-phase hook,
not as `verify`-phase behavior.

Reasoning:

- formatting verification is an early gate, not a packaging concern
- failing fast in `validate` is more useful than failing late in `verify`

Requirements:

- the hook must be easy to disable through config and a property override
- the plugin must probe for the configured Mill target before attempting to run
  it
- target detection should use `mill resolve __` or a narrower resolve command
- if the hook is enabled but the target is missing, the plugin should fail with
  a clear message that explains:
  - Scalafmt verification was requested
  - the expected Mill target was not found
  - how to inspect available targets with `mill resolve __`
  - how to disable the hook with the documented property or config flag

Recommended shape:

- config flag under the Maven `validate` section for Scalafmt verification
- property override such as `-Dmill.interceptor.scalafmt=false`

## Testing Strategy

The implementation should be verified at three levels:

- domain tests
  - config discovery
  - YAML and PKL precedence
  - baseline merging
  - strict failures
  - `MillExecutionPlan` construction
- plugin unit tests
  - Maven request capture
  - Mojo-to-domain translation
  - diagnostics and inspect-plan behavior
- integration tests
  - real Maven fixture builds
  - repo-only config
  - module overrides
  - full lifecycle coverage
  - strict failure paths
  - optional Scalafmt-hook behavior

## Operational Note

Work on this implementation should assume that tracker corruption risk is
increased when worktrees are created or used outside the beads-aware workflow.

Practical rule:

- use `bd worktree create` for tracked feature work that needs beads access
- avoid plain `git worktree add` for tracker-aware work

This should be tracked as an explicit maintainer chore alongside the plugin
implementation work, not left as tribal knowledge.

## Success Criteria

The implementation is successful when it can demonstrate:

- a standard Maven build can forward the full common lifecycle into Mill with
  minimal configuration
- users can override lifecycle and goal behavior predictably at repo and module
  scope
- strict failures remain clear and actionable
- optional Scalafmt validation works when enabled and gives useful guidance
  when unavailable
- README and contributor docs can present a small minimal-configuration story
  without hand-waving
