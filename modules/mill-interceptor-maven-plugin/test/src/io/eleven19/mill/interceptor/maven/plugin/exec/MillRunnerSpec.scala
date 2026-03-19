package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.{EffectiveConfig, MillConfig}
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionMode
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan
import io.eleven19.mill.interceptor.maven.plugin.model.ModuleRef
import io.eleven19.mill.interceptor.maven.plugin.model.PlanStep
import kyo.*
import kyo.Path
import kyo.test.KyoSpecDefault
import zio.test.*

object MillRunnerSpec extends KyoSpecDefault:

    private def request(
        moduleRoot: Path = Path("/repo", "module-a")
    ): ExecutionRequest =
        ExecutionRequest(
            kind = ExecutionRequestKind.LifecyclePhase,
            requestedName = "validate",
            repoRoot = Path("/repo"),
            moduleRoot = moduleRoot,
            module = ModuleRef(
                artifactId = "module-a",
                packaging = "jar"
            )
        )

    def spec: Spec[Any, Any] = suite("MillRunner")(
        test("renders probe steps as mill resolve commands with the module working directory") {
            val plan = MillExecutionPlan(
                request = request(),
                executionMode = ExecutionMode.Strict,
                steps = Seq(PlanStep.ProbeTarget("checkFormat"))
            )

            val rendered = MillRunner.dryRun(plan, EffectiveConfig(mill = MillConfig(executable = "millw")))

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.ProbeTarget,
                    command = Some(Seq("millw", "resolve", "checkFormat")),
                    workingDirectory = Path("/repo", "module-a")
                )
            ))
        },
        test("renders invoke steps as mill commands with configured working directory override") {
            val plan = MillExecutionPlan(
                request = request(),
                executionMode = ExecutionMode.Strict,
                steps = Seq(PlanStep.InvokeMill(Seq("compile", "test")))
            )

            val rendered = MillRunner.dryRun(
                plan,
                EffectiveConfig(
                    mill = MillConfig(
                        executable = "millw",
                        workingDirectory = Some("build")
                    )
                )
            )

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq("millw", "compile", "test")),
                    workingDirectory = Path("/repo", "module-a", "build")
                )
            ))
        },
        test("renders absolute working directory overrides without rebasing") {
            val plan = MillExecutionPlan(
                request = request(),
                executionMode = ExecutionMode.Strict,
                steps = Seq(PlanStep.InvokeMill(Seq("compile")))
            )

            val rendered = MillRunner.dryRun(
                plan,
                EffectiveConfig(
                    mill = MillConfig(
                        executable = "millw",
                        workingDirectory = Some("/opt/mill-work")
                    )
                )
            )

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq("millw", "compile")),
                    workingDirectory = Path("/opt/mill-work")
                )
            ))
        },
        test("renders fail steps without spawning a process") {
            val plan = MillExecutionPlan(
                request = request(),
                executionMode = ExecutionMode.Strict,
                steps = Seq(
                    PlanStep.Fail(
                        message = "No mapping found for explicit goal 'deploy-site' in strict mode",
                        guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                    )
                )
            )

            val rendered = MillRunner.dryRun(plan, EffectiveConfig())

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.Fail,
                    command = None,
                    workingDirectory = Path("/repo", "module-a"),
                    message = Some("No mapping found for explicit goal 'deploy-site' in strict mode"),
                    guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                )
            ))
        },
        suite("execute")(
            test("short-circuits on fail steps without invoking subprocesses") {
                val executor = new RecordingExecutor()
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.Fail(
                            message = "No mapping found for explicit goal 'deploy-site' in strict mode",
                            guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                        ),
                        PlanStep.InvokeMill(Seq("compile"))
                    )
                )

                MillRunner.execute(plan, EffectiveConfig(mill = MillConfig(executable = "millw")), executor).map {
                    case RunnerResult.Failure(stepResults, failure) =>
                        assertTrue(stepResults.isEmpty) &&
                        assertTrue(failure == RunnerFailure.FailStep(
                            message = "No mapping found for explicit goal 'deploy-site' in strict mode",
                            guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                        )) &&
                        assertTrue(executor.calls.isEmpty)
                    case other =>
                        assertTrue(false)
                }
            },
            test("returns probe failures with the probe command and exit code") {
                val executor = new RecordingExecutor(Seq(17))
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(PlanStep.ProbeTarget("checkFormat"))
                )

                MillRunner.execute(plan, EffectiveConfig(mill = MillConfig(executable = "millw")), executor).map {
                    case RunnerResult.Failure(stepResults, failure) =>
                        assertTrue(stepResults.isEmpty) &&
                        assertTrue(failure == RunnerFailure.ProbeFailure(
                            target = "checkFormat",
                            command = Seq("millw", "resolve", "checkFormat"),
                            exitCode = Some(17),
                            message = "Mill target 'checkFormat' is unavailable",
                            guidance = Seq("Run `mill resolve checkFormat` to inspect available targets")
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (Seq("millw", "resolve", "checkFormat"), Path("/repo", "module-a"))
                        ))
                    case other =>
                        assertTrue(false)
                }
            },
            test("returns invocation failures with the invocation command and exit code") {
                val executor = new RecordingExecutor(Seq(0, 9))
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.ProbeTarget("checkFormat"),
                        PlanStep.InvokeMill(Seq("compile", "test"))
                    )
                )

                MillRunner.execute(plan, EffectiveConfig(mill = MillConfig(executable = "millw")), executor).map {
                    case RunnerResult.Failure(stepResults, failure) =>
                        assertTrue(stepResults == Seq(
                            StepResult(
                                kind = RunnerStepKind.ProbeTarget,
                                command = Some(Seq("millw", "resolve", "checkFormat")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(failure == RunnerFailure.InvocationFailure(
                            command = Seq("millw", "compile", "test"),
                            exitCode = Some(9),
                            message = "Mill exited with code 9"
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (Seq("millw", "resolve", "checkFormat"), Path("/repo", "module-a")),
                            (Seq("millw", "compile", "test"), Path("/repo", "module-a"))
                        ))
                    case other =>
                        assertTrue(false)
                }
            },
            test("accumulates ordered step results for successful execution") {
                val executor = new RecordingExecutor(Seq(0, 0))
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.ProbeTarget("checkFormat"),
                        PlanStep.InvokeMill(Seq("compile", "test"))
                    )
                )

                MillRunner.execute(plan, EffectiveConfig(mill = MillConfig(executable = "millw")), executor).map {
                    case RunnerResult.Success(stepResults) =>
                        assertTrue(stepResults == Seq(
                            StepResult(
                                kind = RunnerStepKind.ProbeTarget,
                                command = Some(Seq("millw", "resolve", "checkFormat")),
                                exitCode = Some(0)
                            ),
                            StepResult(
                                kind = RunnerStepKind.InvokeMill,
                                command = Some(Seq("millw", "compile", "test")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (Seq("millw", "resolve", "checkFormat"), Path("/repo", "module-a")),
                            (Seq("millw", "compile", "test"), Path("/repo", "module-a"))
                        ))
                    case other =>
                        assertTrue(false)
                }
            }
        )
    )

    private final class RecordingExecutor(exitCodes: Seq[Int] = Seq.empty) extends MillRunner.SubprocessExecutor:
        private val callsBuffer = scala.collection.mutable.ArrayBuffer.empty[(Seq[String], Path)]

        def calls: Seq[(Seq[String], Path)] = callsBuffer.toSeq

        def run(command: Seq[String], workingDirectory: Path): Int < Sync =
            Sync.defer {
                callsBuffer.append((command, workingDirectory))
                exitCodes.lift(callsBuffer.size - 1).getOrElse(0)
            }
