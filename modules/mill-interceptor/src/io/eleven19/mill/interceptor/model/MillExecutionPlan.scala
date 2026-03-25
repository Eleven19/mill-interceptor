package io.eleven19.mill.interceptor.model

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

/** Execution policy for forwarded requests that do not resolve cleanly. */
enum ExecutionMode derives CanEqual:
    case Strict
    case Hybrid

object ExecutionMode:

    /** Parse persisted config into a supported execution mode. */
    def fromString(value: String): ExecutionMode =
        value.trim.toLowerCase match
            case "hybrid" => Hybrid
            case _        => Strict

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
