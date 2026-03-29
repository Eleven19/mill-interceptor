package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.{EffectiveConfig, MillConfig}
import io.eleven19.mill.interceptor.model.{ExecutionEvent as ModelExecutionEvent}
import io.eleven19.mill.interceptor.model.{ExecutionEventSink as ModelExecutionEventSink}
import io.eleven19.mill.interceptor.model.ExecutionMode
import io.eleven19.mill.interceptor.model.ExecutionRequest
import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.model.MillExecutionPlan
import io.eleven19.mill.interceptor.model.ModuleRef
import io.eleven19.mill.interceptor.model.PlanStep
import kyo.*
import kyo.Path
import kyo.test.KyoSpecDefault
import zio.test.*

object MillRunnerSpec extends KyoSpecDefault:

    private def request(
        moduleRoot: Path = Path("/repo", "module-a"),
        properties: Map[String, String] = Map.empty
    ): ExecutionRequest =
        ExecutionRequest(
            kind = ExecutionRequestKind.LifecyclePhase,
            requestedName = "validate",
            repoRoot = Path("/repo"),
            moduleRoot = moduleRoot,
            module = ModuleRef(
                artifactId = "module-a",
                packaging = "jar"
            ),
            properties = properties
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
                    workingDirectory = Path("/repo")
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
                    workingDirectory = Path("/repo", "build")
                )
            ))
        },
        test("forwards selected Maven system properties into rendered Mill commands") {
            val plan = MillExecutionPlan(
                request = request().copy(
                    requestedName = "install",
                    properties = Map("maven.repo.local" -> "/tmp/m2-repo")
                ),
                executionMode = ExecutionMode.Strict,
                steps = Seq(PlanStep.InvokeMill(Seq("publishM2Local")))
            )

            val rendered = MillRunner.dryRun(
                plan,
                EffectiveConfig(
                    mill = MillConfig(
                        executable = "millw"
                    )
                )
            )

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq("millw", "-Dmaven.repo.local=/tmp/m2-repo", "publishM2Local")),
                    workingDirectory = Path("/repo")
                )
            ))
        },
        test("forwards maven.repo.local into rendered mill commands") {
            val plan = MillExecutionPlan(
                request = request(properties = Map("maven.repo.local" -> "/tmp/m2-repo")),
                executionMode = ExecutionMode.Strict,
                steps = Seq(PlanStep.InvokeMill(Seq("publishM2Local")))
            )

            val rendered = MillRunner.dryRun(plan, EffectiveConfig())

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.InvokeMill,
                    command = Some(Seq("mill", "-Dmaven.repo.local=/tmp/m2-repo", "publishM2Local")),
                    workingDirectory = Path("/repo")
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
                        message = "No mapping found for explicit goal 'deploy-site'",
                        guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                    )
                )
            )

            val rendered = MillRunner.dryRun(plan, EffectiveConfig())

            assertTrue(rendered.steps == Seq(
                DryRunStep(
                    kind = RunnerStepKind.Fail,
                    command = None,
                    workingDirectory = Path("/repo"),
                    message = Some("No mapping found for explicit goal 'deploy-site'"),
                    guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                )
            ))
        },
        suite("execute")(
            test("prefers a module-local mill launcher when the repo root does not provide one") {
                val root = Path("out", "mill-runner-tests", "module-local-launcher")
                val moduleRoot = Path(root, "module-a")
                val launcher = childPath(moduleRoot, "mill")
                val executor = new RecordingExecutor(Seq(0))
                val plan = MillExecutionPlan(
                    request = request(moduleRoot = moduleRoot).copy(repoRoot = Path(root, "repo-root-without-launcher")),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(PlanStep.InvokeMill(Seq("__.compile")))
                )

                for
                    _ <- root.removeAll
                    _ <- moduleRoot.mkDir
                    _ <- launcher.write("#!/usr/bin/env bash\nexit 0\n")
                    result <- MillRunner.execute(plan, EffectiveConfig(), executor)
                    _ <- root.removeAll
                yield result match
                    case RunnerResult.Success(stepResults) =>
                        assertTrue(stepResults == Seq(
                            StepResult(
                                kind = RunnerStepKind.InvokeMill,
                                command = Some(Seq(moduleRoot.toJava.resolve("mill").toString, "__.compile")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq(moduleRoot.toJava.resolve("mill").toString, "__.compile"),
                                Path(root, "repo-root-without-launcher"),
                                Map.empty[String, String]
                            )
                        ))
                    case _ =>
                        assertTrue(false)
            },
            test("prefers a module-local mill launcher for absolute module roots") {
                val root = Path("out", "mill-runner-tests", "absolute-module-local-launcher")
                val absoluteRoot = Path(root.toJava.toAbsolutePath.normalize.toString)
                val moduleRoot = Path(absoluteRoot, "module-a")
                val repoRoot = Path(absoluteRoot, "repo-root-without-launcher")
                val launcher = childPath(moduleRoot, "mill")
                val executor = new RecordingExecutor(Seq(0))
                val plan = MillExecutionPlan(
                    request = request(moduleRoot = moduleRoot).copy(repoRoot = repoRoot),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(PlanStep.InvokeMill(Seq("__.compile")))
                )

                for
                    _ <- absoluteRoot.removeAll
                    _ <- moduleRoot.mkDir
                    _ <- launcher.write("#!/usr/bin/env bash\nexit 0\n")
                    result <- MillRunner.execute(plan, EffectiveConfig(), executor)
                    _ <- absoluteRoot.removeAll
                yield result match
                    case RunnerResult.Success(stepResults) =>
                        assertTrue(stepResults == Seq(
                            StepResult(
                                kind = RunnerStepKind.InvokeMill,
                                command = Some(Seq(moduleRoot.toJava.resolve("mill").toString, "__.compile")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq(moduleRoot.toJava.resolve("mill").toString, "__.compile"),
                                repoRoot,
                                Map.empty[String, String]
                            )
                        ))
                    case _ =>
                        assertTrue(false)
            },
            test("prefers a repo-local mill launcher when no executable override is configured") {
                val root = Path("out", "mill-runner-tests", "local-launcher")
                val launcher = childPath(root, "mill")
                val executor = new RecordingExecutor(Seq(0))
                val plan = MillExecutionPlan(
                    request = request(moduleRoot = Path(root, "module-a")).copy(repoRoot = root),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(PlanStep.InvokeMill(Seq("__.compile")))
                )

                for
                    _ <- root.removeAll
                    _ <- root.mkDir
                    _ <- launcher.write("#!/usr/bin/env bash\nexit 0\n")
                    result <- MillRunner.execute(plan, EffectiveConfig(), executor)
                    _ <- root.removeAll
                yield result match
                    case RunnerResult.Success(stepResults) =>
                        assertTrue(stepResults == Seq(
                            StepResult(
                                kind = RunnerStepKind.InvokeMill,
                                command = Some(Seq(root.toJava.resolve("mill").toString, "__.compile")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq(root.toJava.resolve("mill").toString, "__.compile"),
                                root,
                                Map.empty[String, String]
                            )
                        ))
                    case other =>
                        assertTrue(false)
            },
            test("short-circuits on fail steps without invoking subprocesses") {
                val executor = new RecordingExecutor()
                val sink = ModelExecutionEventSink.recording()
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.Fail(
                            message = "No mapping found for explicit goal 'deploy-site'",
                            guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                        ),
                        PlanStep.InvokeMill(Seq("compile"))
                    )
                )

                MillRunner.execute(
                    plan,
                    EffectiveConfig(mill = MillConfig(executable = "millw")),
                    executor,
                    sink
                ).map {
                    case RunnerResult.Failure(stepResults, failure) =>
                        assertTrue(stepResults.isEmpty) &&
                        assertTrue(failure == RunnerFailure.FailStep(
                            message = "No mapping found for explicit goal 'deploy-site'",
                            guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                        )) &&
                        assertTrue(sink.events == Seq(
                            ModelExecutionEvent.StepFailed(
                                step = PlanStep.Fail(
                                    message = "No mapping found for explicit goal 'deploy-site'",
                                    guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                                ),
                                message = "No mapping found for explicit goal 'deploy-site'",
                                guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                            )
                        )) &&
                        assertTrue(executor.calls.isEmpty)
                    case other =>
                        assertTrue(false)
                }
            },
            test("returns probe failures with the probe command and exit code") {
                val executor = new RecordingExecutor(Seq(17))
                val sink = ModelExecutionEventSink.recording()
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(PlanStep.ProbeTarget("checkFormat"))
                )

                MillRunner.execute(
                    plan,
                    EffectiveConfig(
                        mill = MillConfig(
                            executable = "millw",
                            environment = Map("JAVA_HOME" -> "/opt/java")
                        )
                    ),
                    executor,
                    sink
                ).map {
                    case RunnerResult.Failure(stepResults, failure) =>
                        assertTrue(stepResults.isEmpty) &&
                        assertTrue(failure == RunnerFailure.ProbeFailure(
                            target = "checkFormat",
                            command = Seq("millw", "resolve", "checkFormat"),
                            exitCode = Some(17),
                            message = "Mill target 'checkFormat' is unavailable",
                            guidance = Seq("Run `mill resolve checkFormat` to inspect available targets")
                        )) &&
                        assertTrue(sink.events == Seq(
                            ModelExecutionEvent.StepStarted(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat"))
                            ),
                            ModelExecutionEvent.StepFailed(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat")),
                                exitCode = Some(17),
                                message = "Mill target 'checkFormat' is unavailable",
                                guidance = Seq("Run `mill resolve checkFormat` to inspect available targets")
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq("millw", "resolve", "checkFormat"),
                                Path("/repo"),
                                Map("JAVA_HOME" -> "/opt/java")
                            )
                        ))
                    case other =>
                        assertTrue(false)
                }
            },
            test("returns invocation failures with the invocation command and exit code") {
                val executor = new RecordingExecutor(Seq(0, 9))
                val sink = ModelExecutionEventSink.recording()
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.ProbeTarget("checkFormat"),
                        PlanStep.InvokeMill(Seq("compile", "test"))
                    )
                )

                MillRunner.execute(
                    plan,
                    EffectiveConfig(
                        mill = MillConfig(
                            executable = "millw",
                            environment = Map("MILL_OPTS" -> "--jobs 4")
                        )
                    ),
                    executor,
                    sink
                ).map {
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
                        assertTrue(sink.events == Seq(
                            ModelExecutionEvent.StepStarted(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat"))
                            ),
                            ModelExecutionEvent.StepFinished(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat")),
                                exitCode = Some(0)
                            ),
                            ModelExecutionEvent.StepStarted(
                                step = PlanStep.InvokeMill(Seq("compile", "test")),
                                command = Some(Seq("millw", "compile", "test"))
                            ),
                            ModelExecutionEvent.StepFailed(
                                step = PlanStep.InvokeMill(Seq("compile", "test")),
                                command = Some(Seq("millw", "compile", "test")),
                                exitCode = Some(9),
                                message = "Mill exited with code 9"
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq("millw", "resolve", "checkFormat"),
                                Path("/repo"),
                                Map("MILL_OPTS" -> "--jobs 4")
                            ),
                            (
                                Seq("millw", "compile", "test"),
                                Path("/repo"),
                                Map("MILL_OPTS" -> "--jobs 4")
                            )
                        ))
                    case other =>
                        assertTrue(false)
                }
            },
            test("accumulates ordered step results for successful execution") {
                val executor = new RecordingExecutor(Seq(0, 0))
                val sink = ModelExecutionEventSink.recording()
                val plan = MillExecutionPlan(
                    request = request(),
                    executionMode = ExecutionMode.Strict,
                    steps = Seq(
                        PlanStep.ProbeTarget("checkFormat"),
                        PlanStep.InvokeMill(Seq("compile", "test"))
                    )
                )

                MillRunner.execute(
                    plan,
                    EffectiveConfig(
                        mill = MillConfig(
                            executable = "millw",
                            environment = Map("CI" -> "true")
                        )
                    ),
                    executor,
                    sink
                ).map {
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
                        assertTrue(sink.events == Seq(
                            ModelExecutionEvent.StepStarted(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat"))
                            ),
                            ModelExecutionEvent.StepFinished(
                                step = PlanStep.ProbeTarget("checkFormat"),
                                command = Some(Seq("millw", "resolve", "checkFormat")),
                                exitCode = Some(0)
                            ),
                            ModelExecutionEvent.StepStarted(
                                step = PlanStep.InvokeMill(Seq("compile", "test")),
                                command = Some(Seq("millw", "compile", "test"))
                            ),
                            ModelExecutionEvent.StepFinished(
                                step = PlanStep.InvokeMill(Seq("compile", "test")),
                                command = Some(Seq("millw", "compile", "test")),
                                exitCode = Some(0)
                            )
                        )) &&
                        assertTrue(executor.calls == Seq(
                            (
                                Seq("millw", "resolve", "checkFormat"),
                                Path("/repo"),
                                Map("CI" -> "true")
                            ),
                            (
                                Seq("millw", "compile", "test"),
                                Path("/repo"),
                                Map("CI" -> "true")
                            )
                        ))
                    case other =>
                        assertTrue(false)
                }
            }
        )
    )

    private def childPath(base: Path, child: String): Path =
        Path(base.toJava.resolve(child).toString)

    private final class RecordingExecutor(exitCodes: Seq[Int] = Seq.empty) extends MillRunner.SubprocessExecutor:
        private val callsBuffer = scala.collection.mutable.ArrayBuffer.empty[(Seq[String], Path, Map[String, String])]

        def calls: Seq[(Seq[String], Path, Map[String, String])] = callsBuffer.toSeq

        def run(command: Seq[String], workingDirectory: Path, environment: Map[String, String]): Int < Sync =
            Sync.defer {
                callsBuffer.append((command, workingDirectory, environment))
                exitCodes.lift(callsBuffer.size - 1).getOrElse(0)
            }
