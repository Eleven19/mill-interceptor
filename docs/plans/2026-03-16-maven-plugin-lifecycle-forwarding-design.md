# Maven Plugin Lifecycle Forwarding Design

## Summary

Design the real `mill-interceptor-maven-plugin` so Maven becomes a configurable
entrypoint into Mill. The plugin should forward Maven lifecycle phases and
explicit mapped goals to the external `mill` CLI, default to strict replacement
mode, and support deterministic repository-level and module-level
configuration through YAML and PKL. PKL should be the canonical composition
layer so the plugin can rely on PKL's amend and merge semantics rather than
reimplementing a generic deep-merge engine.

This is design-only work. It does not include implementing the forwarding
engine, YAML or PKL parsing, or lifecycle interception behavior yet.

## Goals

- make Maven a strict-by-default entrypoint into Mill
- forward configured Maven lifecycle phases and explicit goals to Mill
- support both repository-level and module-level configuration
- support both YAML and PKL inputs with deterministic, testable precedence
- invoke Mill as an external process instead of embedding it in-process
- implement the internal runtime in a Kyo-native style consistent with the rest
  of the codebase
- define a test strategy that proves effective config resolution and real Maven
  execution behavior

## Non-Goals

- reimplementing Maven itself or pretending to intercept every arbitrary
  third-party plugin goal in the first version
- embedding Mill as a library or executing it in-process
- introducing heuristic fallback behavior by default
- reactor-wide scheduling optimization in the first implementation
- inventing a custom merge algorithm when PKL can provide the composition model

## Approach Options

### 1. Lifecycle-forwarding plugin with layered configuration

Use lifecycle-bound Mojos and shared forwarding services to resolve Maven
execution into Mill invocations.

Pros:

- aligns with Maven's actual execution model
- supports strict replacement behavior cleanly
- keeps forwarding decisions explicit and testable
- leaves room for future hybrid mode without redesigning the core

Cons:

- requires careful coverage of lifecycle-bound entrypoints
- needs clear diagnostics when strict mode rejects an unmapped execution

### 2. Single catch-all plugin goal

Expose one plugin goal that inspects Maven state and forwards as needed.

Pros:

- smaller implementation surface

Cons:

- does not make `mvn compile`, `mvn test`, or `mvn deploy` behave like real
  entrypoints into Mill
- weak fit for the desired "Maven as entrypoint" behavior

### 3. Deep Maven-core interception

Try to rewrite Maven execution globally through invasive extension-style hooks.

Pros:

- potentially broader interception in theory

Cons:

- higher risk and more coupling to Maven internals
- harder to test and maintain
- too aggressive for the first real implementation

## Recommended Approach

Use option 1. Build a real lifecycle-forwarding Maven plugin that captures
phase and goal execution, resolves the effective forwarding configuration, and
invokes the external `mill` CLI. Default to strict replacement mode, but keep
the model extensible enough for later hybrid or fallback behavior.

## Architecture

The plugin should own three high-level responsibilities:

1. interpret Maven execution context
2. resolve layered YAML and PKL configuration into an effective forwarding
   model
3. execute the resolved Mill invocation externally

The internal architecture should be split into these areas:

- Maven boundary layer
  - Mojos and plugin-specific adapters that capture Maven session, project,
    phase, goal, profiles, and properties
- domain layer
  - pure data models for config, mapping rules, forwarding requests, and the
    resolved `MillExecutionPlan`
- config loading and evaluation layer
  - YAML parsing, PKL evaluation, config discovery, precedence handling, and
    validation
- resolution layer
  - translates Maven execution context plus effective config into a
    `MillExecutionPlan`
- execution layer
  - runs the resolved plan through the external `mill` CLI and maps results
    back into Maven success or failure semantics

The existing placeholder Maven plugin module remains the correct home for this
work:

- module: `modules/mill-interceptor-maven-plugin`
- published coordinate:
  `io.eleven19.mill-interceptor:mill-interceptor-maven-plugin:<version>`

## Runtime Model

### Default Mode

The plugin should start in strict replacement mode:

- if a Maven phase or explicit mapped goal is configured, forward it to Mill
- if a Maven phase or explicit mapped goal is not configured, fail the build
  with a clear diagnostic

The overall mode should remain configurable so a later hybrid mode can allow
explicit fallback to native Maven behavior.

### Maven Interception Surface

The first real implementation should focus on lifecycle-bound Mojos for the
core phases that matter to normal builds:

- `clean`
- `validate`
- `compile`
- `test`
- `package`
- `verify`
- `install`
- `deploy`

The plugin should also expose explicit plugin goals for operational use, such
as:

- printing the resolved config
- printing the resolved `MillExecutionPlan`
- debugging or dry-running the mapped Mill invocation

The first implementation should not promise transparent interception of every
third-party Maven plugin goal. It should focus on Maven lifecycle execution and
explicit goal mappings defined by the plugin's own configuration model.

## Configuration Model

### Supported File Names

Primary file names:

- `mill-interceptor.yaml`
- `mill-interceptor.pkl`

Alternate config-directory locations:

- `.config/mill-interceptor/config.yaml`
- `.config/mill-interceptor/config.pkl`

These names are intentionally tool-neutral. The config schema should contain
tool-specific sections so the same file family can express Maven-related
configuration without encoding "maven" into the filename.

### Discovery And Precedence

Config discovery should be deterministic and testable. The approved discovery
order is:

1. repository root `mill-interceptor.yaml`
2. repository root `mill-interceptor.pkl`
3. repository `.config/mill-interceptor/config.yaml`
4. repository `.config/mill-interceptor/config.pkl`
5. module-local `mill-interceptor.yaml`
6. module-local `mill-interceptor.pkl`
7. module-local `.config/mill-interceptor/config.yaml`
8. module-local `.config/mill-interceptor/config.pkl`

This preserves two rules:

- broader scope before narrower scope
- YAML before PKL within each location pair

### YAML And PKL Roles

YAML and PKL are both supported, but they do not play identical roles:

- YAML is a data input format for straightforward static mappings
- PKL is the canonical composition layer

The plugin should parse YAML into the canonical config model, expose those
values to PKL evaluation, and let PKL perform the meaningful layering and amend
behavior. The plugin should not implement a generic nested deep-merge engine
when PKL can provide the composition model directly.

The plugin still owns:

- file discovery order
- scope ordering
- validation of the final evaluated config
- diagnostics about conflicting or malformed inputs

### Config Concepts

The effective config model should include, at minimum:

- mode
  - `strict` by default, with room for a later `hybrid` mode
- Maven section
  - lifecycle phase mappings
  - explicit goal mappings
  - module selectors or overrides
  - fallback policy
  - property and profile propagation rules
- Mill execution settings
  - executable path
  - working-directory strategy
  - environment additions or overrides
- diagnostics settings
  - debug verbosity
  - dry-run support

## Forwarding And Execution

The plugin should resolve each Maven execution into a `MillExecutionPlan` and
then run that plan through the external `mill` CLI.

The resolved plan should capture, at minimum:

- execution kind
  - lifecycle phase or explicit goal
- Maven module identity
  - groupId, artifactId, packaging, basedir
- resolved Mill invocation
  - command, task targets, or arguments
- forwarded properties and profiles
- execution policy
  - strict failure, and later optional fallback or skip behavior

### Kyo-Native Implementation

The plugin must use Kyo as a first-class part of the implementation style. The
external process boundary remains, but the code around it should be effectful
and functional in the same style as the rest of the codebase.

Kyo should be used for:

- process construction and execution
- command modeling
- environment and system interaction
- structured error propagation
- effectful diagnostics and logging orchestration
- future timeout, cancellation, and orchestration behavior

The execution stack should look like this:

1. Mojo boundary captures Maven execution context
2. boundary code translates that context into a domain request
3. a Kyo-backed forwarding service loads config, evaluates PKL, resolves the
   `MillExecutionPlan`, and executes it
4. the final result is translated back into Maven plugin semantics

Scribe should remain the logging layer for diagnostics emitted by the plugin and
its forwarding services.

### Process Behavior

The external `mill` invocation should:

- be built as an argument vector, not a shell string
- use a configured or default executable path
- run from a configured working directory strategy
- stream stdout and stderr in a way that remains legible in Maven output
- map non-zero exit codes to Maven build failure
- support a dry-run or inspect mode that prints the resolved plan without
  executing Mill

### Reactor Scope

The first implementation should plan around per-module execution in the Maven
reactor. That is the simplest reliable model. Reactor-wide batching or global
coalescing of Mill invocations should be treated as future optimization work.

## Shared Planning Model

The `MillExecutionPlan` should be treated as a real domain type, not Maven-only
glue. The Maven plugin will need it immediately, but the existing
`mill-interceptor` CLI is a plausible consumer too.

The preferred dependency direction is:

- shared planning or domain layer
- entrypoint adapters on top
  - Maven plugin
  - CLI
  - future build-tool adapters

This follow-up has been captured as bead issue `MI-szk`: evaluate extracting or
sharing the plan model so the Maven plugin and CLI do not grow duplicate
planning logic.

## Testing Strategy

The first implementation should be tested at three levels.

### 1. Domain Tests

Cover:

- config discovery order
- YAML parsing
- PKL evaluation and amend behavior
- scope precedence
- lifecycle and goal mapping resolution
- strict-mode failure cases
- `MillExecutionPlan` construction

### 2. Plugin Unit Tests

Cover:

- Mojo translation of Maven context into forwarding requests
- plugin-specific diagnostics and error mapping
- inspect or debug goal output

### 3. Integration Tests

Use fixture Maven projects to prove:

- repository-only config works
- module-local overrides work
- YAML and PKL precedence behaves as designed
- strict mode fails cleanly on unmapped execution
- Maven can invoke the plugin and the plugin can invoke the external `mill`
  process with the expected command shape

## Rollout Boundaries

The first implementation should include:

- lifecycle forwarding for core Maven phases
- explicit mapped goal support
- repository-level and module-level config layering
- YAML input and PKL composition
- strict mode by default
- clear diagnostics and inspect or dry-run support

It should explicitly exclude:

- transparent support for arbitrary third-party Maven plugin-goal ecosystems
- in-process Mill embedding
- heuristic fallback behavior
- reactor-wide execution optimization

## Success Criteria

The design is successful when the implementation can later demonstrate that:

- `mvn compile`, `mvn test`, `mvn package`, `mvn install`, and `mvn deploy`
  can act as entrypoints into Mill through explicit configuration
- effective config precedence across repo or module and YAML or PKL is
  deterministic and covered by tests
- strict mode fails with clear, actionable diagnostics when a phase or goal is
  unmapped
- the plugin can print or inspect the resolved forwarding plan for debugging

## Risks

- Maven lifecycle interception is easy to overclaim, so the first version must
  keep its boundary explicit
- PKL integration introduces schema and runtime evaluation considerations that
  must be validated carefully
- per-module reactor execution is simpler but may expose performance pressure in
  larger builds later
- config discovery across multiple file locations can become opaque without good
  diagnostics
