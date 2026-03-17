# Kyo Filesystem Sweep Design

## Summary

Refactor module application and test code to follow the repository's filesystem
and process guidance: prefer Kyo first, then `os-lib`, and reserve raw Java NIO
for narrow interop boundaries. Keep `mill-build/` on `os-lib`, since that is
the natural abstraction for Mill build code.

## Scope

This design applies to Scala code under `modules/`:

- `modules/mill-interceptor`
- `modules/mill-interceptor-maven-plugin`

It does not require sweeping `mill-build/` away from `os-lib`.

## Architecture

### Application and Library Code

Application and library code should use Kyo as the primary effect and runtime
boundary for filesystem and process work.

- Prefer Kyo path types and operations for repo-owned filesystem logic
- Prefer Kyo process abstractions for external command execution
- Use `os-lib` only if Kyo cannot express the need cleanly
- Use raw `java.nio.file` only where an external API forces that boundary

### Mill Build Code

Mill build code should continue preferring `os-lib` directly.

- Do not force Kyo into `mill-build/`
- Treat `os-lib` as the native abstraction for Mill tasks and packaging code

## Rollout

Implement this in two commits.

### Commit 1: CLI and Shim Layer

Refactor the CLI and shim generation code to Kyo-first filesystem types and
operations.

Targets:

- `modules/mill-interceptor/src/io/eleven19/mill/interceptor/Cli.scala`
- `modules/mill-interceptor/src/io/eleven19/mill/interceptor/shim/ShimGenerator.scala`
- related tests under `modules/mill-interceptor/test/...`

Goals:

- remove raw Java NIO from repo-owned CLI and shim logic
- stop leaking `java.nio.file.Path` through the public shim option and result
  types
- keep behavior unchanged

### Commit 2: Maven Plugin Integration Test Layer

Refactor the Maven plugin integration test helpers to Kyo-first filesystem and
process handling.

Targets:

- `modules/mill-interceptor-maven-plugin/itest/src/io/eleven19/mill/interceptor/maven/plugin/MavenPluginIntegrationSpec.scala`

Goals:

- replace raw Java NIO temp-dir, copy, PATH lookup, and cleanup logic
- replace `scala.sys.process` with Kyo-owned command execution
- keep the existing integration-test behavior unchanged

## Testing Strategy

Use TDD for both slices.

### CLI and Shim Layer

- update or add failing tests first
- add filesystem-writing coverage for shim generation through the new Kyo-backed
  implementation
- iterate on the relevant CLI and shim test targets before running the full
  module test suite

### Maven Plugin Integration Test Layer

- update the helper expectations first
- refactor the implementation second
- rerun the Maven plugin integration suite after the helper change

## Success Criteria

- module application and test code no longer relies on raw `java.nio.file`
  except at required interop seams
- `scala.sys.process` is removed from module code where Kyo can own the process
  boundary
- existing CLI, shim, and Maven plugin integration behavior remains unchanged
- `mill-build/` remains on `os-lib`
- contributor guidance is updated to clarify the distinction between application
  code and Mill build code
