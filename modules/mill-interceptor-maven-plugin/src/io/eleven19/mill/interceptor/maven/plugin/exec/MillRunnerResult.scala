package io.eleven19.mill.interceptor.maven.plugin.exec

/** The kind of runner step that completed or failed. */
enum RunnerStepKind derives CanEqual:
    case ProbeTarget
    case InvokeMill
    case Fail

/** The final status of a Mill runner execution. */
enum RunnerStatus derives CanEqual:
    case Success
    case Failure

/** Structured metadata for one completed runner step. */
final case class StepResult(
    kind: RunnerStepKind,
    command: Option[Seq[String]] = None,
    exitCode: Option[Int] = None
) derives CanEqual

/** Structured metadata for a terminal runner failure. */
final case class RunnerFailure(
    kind: RunnerStepKind,
    message: String,
    guidance: Seq[String] = Seq.empty,
    command: Option[Seq[String]] = None,
    exitCode: Option[Int] = None
) derives CanEqual

/** Summary of a runner invocation over a resolved Mill execution plan. */
final case class RunnerResult(
    status: RunnerStatus,
    stepResults: Seq[StepResult],
    failure: Option[RunnerFailure] = None
) derives CanEqual
