package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan
import io.eleven19.mill.interceptor.maven.plugin.model.PlanStep
import kyo.Path

/** A rendered command and its working directory for dry-run inspection. */
final case class RenderedCommand(
    command: Seq[String],
    workingDirectory: Path
) derives CanEqual

/** A rendered dry-run step that mirrors a `PlanStep` without spawning a process. */
final case class DryRunStep(
    kind: RunnerStepKind,
    command: Option[Seq[String]] = None,
    workingDirectory: Path,
    message: Option[String] = None,
    guidance: Seq[String] = Seq.empty
) derives CanEqual

/** Dry-run output for a plan. */
final case class DryRunResult(
    steps: Seq[DryRunStep]
) derives CanEqual

object MillRunner:

    def dryRun(plan: MillExecutionPlan, config: EffectiveConfig): DryRunResult =
        DryRunResult(
            steps = plan.steps.map(renderStep(_, plan.request, config))
        )

    private def renderStep(
        step: PlanStep,
        request: ExecutionRequest,
        config: EffectiveConfig
    ): DryRunStep =
        val workingDirectory = resolveWorkingDirectory(request.moduleRoot, config.mill.workingDirectory)
        step match
            case PlanStep.ProbeTarget(target) =>
                DryRunStep(
                    kind = RunnerStepKind.ProbeTarget,
                    command = Some(Seq(config.mill.executable, "resolve", target)),
                    workingDirectory = workingDirectory
                )
            case PlanStep.InvokeMill(targets) =>
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq(config.mill.executable) ++ targets),
                    workingDirectory = workingDirectory
                )
            case PlanStep.Fail(message, guidance) =>
                DryRunStep(
                    kind = RunnerStepKind.Fail,
                    message = Some(message),
                    workingDirectory = workingDirectory,
                    guidance = guidance
                )

    private def resolveWorkingDirectory(base: Path, overridePath: Option[String]): Path =
        overridePath match
            case Some(value) if value.nonEmpty && value.startsWith("/") => Path(value)
            case Some(value) if value.nonEmpty                          => Path(base.toJava.resolve(value).toString)
            case _                                                      => base
