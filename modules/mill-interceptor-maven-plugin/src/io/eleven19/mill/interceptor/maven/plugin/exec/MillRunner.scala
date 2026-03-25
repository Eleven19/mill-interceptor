package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan
import io.eleven19.mill.interceptor.maven.plugin.model.PlanStep
import kyo.*

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

    /** Small injectable subprocess boundary for plan execution. */
    trait SubprocessExecutor:
        def run(command: Seq[String], workingDirectory: Path, environment: Map[String, String]): Int < Sync

    object SubprocessExecutor:

        val live: SubprocessExecutor = new SubprocessExecutor:
            def run(command: Seq[String], workingDirectory: Path, environment: Map[String, String]): Int < Sync =
                direct {
                    Process
                        .Command(command*)
                        .cwd(workingDirectory.toJava)
                        .env(environment)
                        .stdin(Process.Input.Inherit)
                        .stdout(Process.Output.Inherit)
                        .stderr(Process.Output.Inherit)
                        .waitFor
                        .now
                }

    def dryRun(plan: MillExecutionPlan, config: EffectiveConfig): DryRunResult =
        DryRunResult(
            steps = plan.steps.map(renderStep(_, plan.request, config))
        )

    def execute(
        plan: MillExecutionPlan,
        config: EffectiveConfig,
        executor: SubprocessExecutor = SubprocessExecutor.live
    ): RunnerResult < Sync =
        for
            executable <- resolveExecutable(plan.request, config)
            forwardedArgs = forwardedPropertyArgs(plan.request)
            result <- executeSteps(
                plan.steps,
                Vector.empty,
                resolveWorkingDirectory(plan.request.moduleRoot, config.mill.workingDirectory),
                executable,
                forwardedArgs,
                config,
                executor,
                config.mill.environment
            )
        yield result

    private def executeSteps(
        remaining: Seq[PlanStep],
        completed: Vector[StepResult],
        workingDirectory: Path,
        executable: String,
        forwardedArgs: Seq[String],
        config: EffectiveConfig,
        executor: SubprocessExecutor,
        environment: Map[String, String]
    ): RunnerResult < Sync =
        remaining.headOption match
            case None =>
                Sync.defer(RunnerResult.Success(completed.toSeq))
            case Some(PlanStep.Fail(message, guidance)) =>
                Sync.defer(
                    RunnerResult.Failure(
                        completed.toSeq,
                        RunnerFailure.FailStep(message, guidance)
                    )
                )
            case Some(PlanStep.ProbeTarget(target)) =>
                val command = Seq(executable) ++ forwardedArgs ++ Seq("resolve", target)
                for
                    exitCode <- executor.run(command, workingDirectory, environment)
                    result <-
                        if exitCode == 0 then
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
                                environment
                            )
                        else
                            Sync.defer(
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
                            )
                yield result
            case Some(PlanStep.InvokeMill(targets)) =>
                val command = Seq(executable) ++ forwardedArgs ++ targets
                for
                    exitCode <- executor.run(command, workingDirectory, environment)
                    result <-
                        if exitCode == 0 then
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
                                environment
                            )
                        else
                            Sync.defer(
                                RunnerResult.Failure(
                                    completed.toSeq,
                                    RunnerFailure.InvocationFailure(
                                        command = command,
                                        exitCode = Some(exitCode),
                                        message = s"Mill exited with code $exitCode"
                                    )
                                )
                            )
                yield result

    private def renderStep(
        step: PlanStep,
        request: ExecutionRequest,
        config: EffectiveConfig
    ): DryRunStep =
        val workingDirectory = resolveWorkingDirectory(request.moduleRoot, config.mill.workingDirectory)
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

    private def resolveWorkingDirectory(base: Path, overridePath: Option[String]): Path =
        overridePath match
            case Some(value) if value.nonEmpty =>
                val overrideJava = java.nio.file.Paths.get(value)
                if overrideJava.isAbsolute then Path(overrideJava.toString)
                else Path(base.toJava.resolve(overrideJava).toString)
            case _ => base

    private def resolveExecutable(request: ExecutionRequest, config: EffectiveConfig): String < Sync =
        if config.mill.executable != "mill" then Sync.defer(config.mill.executable)
        else
            val launcherCandidates = Seq(
                request.moduleRoot.toJava.resolve("mill"),
                request.moduleRoot.toJava.resolve("millw"),
                request.repoRoot.toJava.resolve("mill"),
                request.repoRoot.toJava.resolve("millw")
            )
            for candidates <- Kyo.foreach(launcherCandidates) { candidate =>
                    Sync.defer(candidate.toFile.exists()).map(exists => candidate -> exists)
                }
            yield candidates.collectFirst { case (candidate, true) => candidate.toString }.getOrElse("mill")

    private def forwardedPropertyArgs(request: ExecutionRequest): Seq[String] =
        request.properties.get("maven.repo.local").toSeq.map(value => s"-Dmaven.repo.local=$value")
