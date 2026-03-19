package io.eleven19.mill.interceptor.maven.plugin.exec

import io.eleven19.mill.interceptor.maven.plugin.config.{EffectiveConfig, MillConfig}
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionMode
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan
import io.eleven19.mill.interceptor.maven.plugin.model.ModuleRef
import io.eleven19.mill.interceptor.maven.plugin.model.PlanStep
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
        }
    )
