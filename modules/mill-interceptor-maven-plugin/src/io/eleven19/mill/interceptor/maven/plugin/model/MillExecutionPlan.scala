package io.eleven19.mill.interceptor.maven.plugin.model

import kyo.Path

/** Identifies whether the incoming request came from a lifecycle phase or an explicit goal. */
enum ExecutionRequestKind derives CanEqual:
    case LifecyclePhase
    case ExplicitGoal

/** Minimal module identity needed for request resolution. */
final case class ModuleRef(
    artifactId: String,
    packaging: String,
    groupId: Option[String] = None
) derives CanEqual

/** Neutral execution input for building a Mill plan from a tool-specific request. */
final case class ExecutionRequest(
    kind: ExecutionRequestKind,
    requestedName: String,
    repoRoot: Path,
    moduleRoot: Path,
    module: ModuleRef,
    properties: Map[String, String] = Map.empty
) derives CanEqual

/** Ordered execution steps for a single resolved request. */
enum PlanStep derives CanEqual:
    case ProbeTarget(target: String)
    case InvokeMill(targets: Seq[String])
    case Fail(message: String, guidance: Seq[String] = Seq.empty)

/** Multi-step execution plan resolved from configuration, defaults, and request context. */
final case class MillExecutionPlan(
    request: ExecutionRequest,
    executionMode: ExecutionMode,
    steps: Seq[PlanStep]
) derives CanEqual
