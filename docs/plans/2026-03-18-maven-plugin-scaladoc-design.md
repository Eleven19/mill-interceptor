# Maven Plugin Scaladoc Design

## Summary

Add concise Scaladoc to the public lifecycle-baseline and effective-config types on
`mi-okw-4-lifecycle-baseline`.

## Scope

- Document the public config types in
  `modules/mill-interceptor-maven-plugin/.../config/EffectiveConfig.scala`
- Document the public lifecycle model types in
  `modules/mill-interceptor-maven-plugin/.../model/LifecycleBaseline.scala`
- Document the execution mode enum in
  `modules/mill-interceptor-maven-plugin/.../model/ExecutionMode.scala`
- Create a separate beads task for a broader Maven plugin Scaladoc sweep

## Non-Goals

- No repo-wide Scaladoc cleanup
- No behavior changes
- No test-only comment churn

## Style

Keep comments short and focused on:
- what the type represents
- defaults or precedence that matter
- what resolver methods return
