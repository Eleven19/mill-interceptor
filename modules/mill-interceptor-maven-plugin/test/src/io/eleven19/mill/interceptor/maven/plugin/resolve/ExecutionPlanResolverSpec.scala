package io.eleven19.mill.interceptor.maven.plugin.resolve

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.model.*
import kyo.Path
import kyo.test.KyoSpecDefault
import zio.test.*

object ExecutionPlanResolverSpec extends KyoSpecDefault:

    private def request(
        kind: ExecutionRequestKind,
        requestedName: String,
        properties: Map[String, String] = Map.empty
    ): ExecutionRequest =
        ExecutionRequest(
            kind = kind,
            requestedName = requestedName,
            repoRoot = Path("/repo"),
            moduleRoot = Path("/repo", "module-a"),
            module = ModuleRef(
                artifactId = "module-a",
                packaging = "jar"
            ),
            properties = properties
        )

    def spec: Spec[Any, Any] = suite("ExecutionPlanResolver")(
        test("resolves lifecycle phases from the conventional baseline") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.LifecyclePhase, "compile"),
                config = EffectiveConfig()
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.InvokeMill(Seq("compile"))
                )
            )
        },
        test("resolves explicit goals from goal mappings") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.ExplicitGoal, "inspect-plan"),
                config = EffectiveConfig(
                    goals = Map("inspect-plan" -> Seq("interceptor.inspect"))
                )
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.InvokeMill(Seq("interceptor.inspect"))
                )
            )
        },
        test("produces strict failures for unmapped explicit goals") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.ExplicitGoal, "deploy-site"),
                config = EffectiveConfig()
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.Fail(
                        message = "No mapping found for explicit goal 'deploy-site'",
                        guidance = Seq(
                            "Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl"
                        )
                    )
                )
            )
        },
        test("produces strict failures for unsupported lifecycle phases") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.LifecyclePhase, "site"),
                config = EffectiveConfig()
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.Fail(
                        message = "No mapping found for lifecycle phase 'site'",
                        guidance = Seq(
                            "Add a lifecycle mapping in mill-interceptor.yaml or mill-interceptor.pkl"
                        )
                    )
                )
            )
        },
        test("prefers explicit lifecycle overrides over the baseline") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.LifecyclePhase, "test"),
                config = EffectiveConfig(
                    lifecycle = Map("test" -> Seq("core.compile", "core.test"))
                )
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.InvokeMill(Seq("core.compile", "core.test"))
                )
            )
        },
        test("adds validate scalafmt probe and invocation before validate mappings") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(ExecutionRequestKind.LifecyclePhase, "validate"),
                config = EffectiveConfig(
                    lifecycle = Map("validate" -> Seq("app.validate"))
                )
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.ProbeTarget("mill.scalalib.scalafmt/checkFormatAll"),
                    PlanStep.InvokeMill(Seq("mill.scalalib.scalafmt/checkFormatAll")),
                    PlanStep.InvokeMill(Seq("app.validate"))
                )
            )
        },
        test("allows validate scalafmt hook to be disabled by property override") {
            val plan = ExecutionPlanResolver.resolve(
                request = request(
                    kind = ExecutionRequestKind.LifecyclePhase,
                    requestedName = "validate",
                    properties = Map("mill.interceptor.scalafmt" -> "false")
                ),
                config = EffectiveConfig(
                    lifecycle = Map("validate" -> Seq("app.validate"))
                )
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.InvokeMill(Seq("app.validate"))
                )
            )
        }
    )
