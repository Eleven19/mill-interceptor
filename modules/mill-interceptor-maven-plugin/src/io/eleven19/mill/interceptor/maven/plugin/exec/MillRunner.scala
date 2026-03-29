package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.model.ExecutionRequest
import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.model.ExecutionEvent
import io.eleven19.mill.interceptor.model.ExecutionEventSink
import io.eleven19.mill.interceptor.model.MillExecutionPlan
import io.eleven19.mill.interceptor.model.PlanStep

/** A rendered command and its working directory for dry-run inspection. */
final case class RenderedCommand(
    command: Seq[String],
    workingDirectory: os.Path
) derives CanEqual

/** A rendered dry-run step that mirrors a `PlanStep` without spawning a process. */
final case class DryRunStep(
    kind: RunnerStepKind,
    command: Option[Seq[String]] = None,
    workingDirectory: os.Path,
    message: Option[String] = None,
    guidance: Seq[String] = Seq.empty
) derives CanEqual

/** Dry-run output for a plan. */
final case class DryRunResult(
    steps: Seq[DryRunStep]
) derives CanEqual

/** Executes resolved Mill plans or renders them for inspect-only flows. */
object MillRunner:

    /** Small injectable subprocess boundary for plan execution. */
    trait SubprocessExecutor:
        def run(command: Seq[String], workingDirectory: os.Path, environment: Map[String, String]): Int

    object SubprocessExecutor:

        val live: SubprocessExecutor = new SubprocessExecutor:
            def run(command: Seq[String], workingDirectory: os.Path, environment: Map[String, String]): Int =
                val result = os.proc(command).call(
                    cwd = workingDirectory,
                    env = environment,
                    stdin = os.Inherit,
                    stdout = os.Inherit,
                    stderr = os.Inherit,
                    check = false
                )
                result.exitCode

    /** Render a plan without running subprocesses. */
    def dryRun(plan: MillExecutionPlan, config: EffectiveConfig): DryRunResult =
        DryRunResult(
            steps = plan.steps.map(renderStep(_, plan.request, config))
        )

    /** Execute a plan step-by-step and stop on the first terminal failure. */
    def execute(
        plan: MillExecutionPlan,
        config: EffectiveConfig,
        executor: SubprocessExecutor = SubprocessExecutor.live,
        sink: ExecutionEventSink = ExecutionEventSink.noop
    ): RunnerResult =
        val executable   = resolveExecutable(plan.request, config)
        val forwardedArgs = forwardedPropertyArgs(plan.request)
        executeSteps(
            plan.steps,
            Vector.empty,
            resolveWorkingDirectory(plan.request.repoRoot, config.mill.workingDirectory),
            executable,
            forwardedArgs,
            config,
            executor,
            config.mill.environment,
            sink
        )

    private def executeSteps(
        remaining: Seq[PlanStep],
        completed: Vector[StepResult],
        workingDirectory: os.Path,
        executable: String,
        forwardedArgs: Seq[String],
        config: EffectiveConfig,
        executor: SubprocessExecutor,
        environment: Map[String, String],
        sink: ExecutionEventSink
    ): RunnerResult =
        remaining.headOption match
            case None =>
                RunnerResult.Success(completed.toSeq)
            case Some(PlanStep.Fail(message, guidance)) =>
                sink.publish(
                    ExecutionEvent.StepFailed(
                        step = PlanStep.Fail(message, guidance),
                        message = message,
                        guidance = guidance
                    )
                )
                RunnerResult.Failure(
                    completed.toSeq,
                    RunnerFailure.FailStep(message, guidance)
                )
            case Some(PlanStep.ProbeTarget(target)) =>
                val command = Seq(executable) ++ forwardedArgs ++ Seq("resolve", target)
                val step    = PlanStep.ProbeTarget(target)
                sink.publish(ExecutionEvent.StepStarted(step = step, command = Some(command)))
                val exitCode = executor.run(command, workingDirectory, environment)
                if exitCode == 0 then
                    sink.publish(
                        ExecutionEvent.StepFinished(
                            step = step,
                            command = Some(command),
                            exitCode = Some(exitCode)
                        )
                    )
                    executeSteps(
                        remaining.tail,
                        completed :+ StepResult(
                            kind = RunnerStepKind.ProbeTarget,
                            command = Some(command),
                            exitCode = Some(exitCode)
                        ),
                        workingDirectory,
                        executable,
                        forwardedArgs,
                        config,
                        executor,
                        environment,
                        sink
                    )
                else
                    sink.publish(
                        ExecutionEvent.StepFailed(
                            step = step,
                            command = Some(command),
                            exitCode = Some(exitCode),
                            message = s"Mill target '$target' is unavailable",
                            guidance = Seq(s"Run `mill resolve $target` to inspect available targets")
                        )
                    )
                    RunnerResult.Failure(
                        completed.toSeq,
                        RunnerFailure.ProbeFailure(
                            target = target,
                            command = command,
                            exitCode = Some(exitCode),
                            message = s"Mill target '$target' is unavailable",
                            guidance = Seq(s"Run `mill resolve $target` to inspect available targets")
                        )
                    )
            case Some(PlanStep.InvokeMill(targets)) =>
                val command = Seq(executable) ++ forwardedArgs ++ targets
                val step    = PlanStep.InvokeMill(targets)
                sink.publish(ExecutionEvent.StepStarted(step = step, command = Some(command)))
                val exitCode = executor.run(command, workingDirectory, environment)
                if exitCode == 0 then
                    sink.publish(
                        ExecutionEvent.StepFinished(
                            step = step,
                            command = Some(command),
                            exitCode = Some(exitCode)
                        )
                    )
                    executeSteps(
                        remaining.tail,
                        completed :+ StepResult(
                            kind = RunnerStepKind.InvokeMill,
                            command = Some(command),
                            exitCode = Some(exitCode)
                        ),
                        workingDirectory,
                        executable,
                        forwardedArgs,
                        config,
                        executor,
                        environment,
                        sink
                    )
                else
                    sink.publish(
                        ExecutionEvent.StepFailed(
                            step = step,
                            command = Some(command),
                            exitCode = Some(exitCode),
                            message = s"Mill exited with code $exitCode"
                        )
                    )
                    RunnerResult.Failure(
                        completed.toSeq,
                        RunnerFailure.InvocationFailure(
                            command = command,
                            exitCode = Some(exitCode),
                            message = s"Mill exited with code $exitCode"
                        )
                    )

    private def renderStep(
        step: PlanStep,
        request: ExecutionRequest,
        config: EffectiveConfig
    ): DryRunStep =
        val workingDirectory = resolveWorkingDirectory(request.repoRoot, config.mill.workingDirectory)
        val forwardedArgs    = forwardedPropertyArgs(request)
        step match
            case PlanStep.ProbeTarget(target) =>
                DryRunStep(
                    kind = RunnerStepKind.ProbeTarget,
                    command = Some(Seq(config.mill.executable) ++ forwardedArgs ++ Seq("resolve", target)),
                    workingDirectory = workingDirectory
                )
            case PlanStep.InvokeMill(targets) =>
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq(config.mill.executable) ++ forwardedArgs ++ targets),
                    workingDirectory = workingDirectory
                )
            case PlanStep.Fail(message, guidance) =>
                DryRunStep(
                    kind = RunnerStepKind.Fail,
                    message = Some(message),
                    workingDirectory = workingDirectory,
                    guidance = guidance
                )

    private def resolveWorkingDirectory(base: os.Path, overridePath: Option[String]): os.Path =
        overridePath match
            case Some(value) if value.nonEmpty =>
                val configured = os.Path(java.nio.file.Paths.get(value))
                if configured.toNIO.isAbsolute then configured
                else os.Path(base.toNIO.resolve(configured.toNIO))
            case _ => base

    private def resolveExecutable(request: ExecutionRequest, config: EffectiveConfig): String =
        if config.mill.executable != "mill" then config.mill.executable
        else
            val launcherCandidates = Seq(
                request.moduleRoot / "mill",
                request.moduleRoot / "millw",
                request.repoRoot / "mill",
                request.repoRoot / "millw"
            )
            launcherCandidates.find(os.exists).map(_.toString).getOrElse("mill")

    private def forwardedPropertyArgs(request: ExecutionRequest): Seq[String] =
        request.properties.get("maven.repo.local").toSeq.map(value => s"-Dmaven.repo.local=$value")
