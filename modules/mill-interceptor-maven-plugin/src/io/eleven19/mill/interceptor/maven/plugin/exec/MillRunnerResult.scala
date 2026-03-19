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
sealed trait RunnerFailure derives CanEqual:

    def kind: RunnerStepKind
    def message: String
    def guidance: Seq[String]

object RunnerFailure:

    /** Failure caused by an explicit fail step in the plan. */
    final case class FailStep(
        message: String,
        guidance: Seq[String] = Seq.empty
    ) extends RunnerFailure:
        val kind = RunnerStepKind.Fail

    /** Failure caused by an unavailable Mill target probe. */
    final case class ProbeFailure(
        target: String,
        command: Seq[String],
        exitCode: Option[Int],
        message: String,
        guidance: Seq[String] = Seq.empty
    ) extends RunnerFailure:
        val kind = RunnerStepKind.ProbeTarget

    /** Failure caused by a non-zero Mill invocation exit. */
    final case class InvocationFailure(
        command: Seq[String],
        exitCode: Option[Int],
        message: String,
        guidance: Seq[String] = Seq.empty
    ) extends RunnerFailure:
        val kind = RunnerStepKind.InvokeMill

/** Structured outcome of a runner invocation. */
sealed trait RunnerResult derives CanEqual:

    def stepResults: Seq[StepResult]

object RunnerResult:

    /** Successful plan execution with all steps completed. */
    final case class Success(stepResults: Seq[StepResult]) extends RunnerResult

    /** Failed plan execution with the terminal failure attached. */
    final case class Failure(stepResults: Seq[StepResult], failure: RunnerFailure) extends RunnerResult
