package io.eleven19.mill.interceptor.maven.plugin.model

import io.eleven19.mill.interceptor.model.*
import kyo.Path
import kyo.test.KyoSpecDefault
import zio.test.*

object MillExecutionPlanSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("MillExecutionPlan")(
        test("models lifecycle requests as ordered execution plans") {
            val request = ExecutionRequest(
                kind = ExecutionRequestKind.LifecyclePhase,
                requestedName = "validate",
                repoRoot = Path("/repo"),
                moduleRoot = Path("/repo", "module-a"),
                module = ModuleRef(
                    artifactId = "module-a",
                    packaging = "jar"
                ),
                properties = Map("mill.interceptor.scalafmt" -> "true")
            )

            val plan = MillExecutionPlan(
                request = request,
                executionMode = ExecutionMode.Strict,
                steps = Seq(
                    PlanStep.ProbeTarget("__.checkFormat"),
                    PlanStep.InvokeMill(Seq("__.checkFormat"))
                )
            )

            assertTrue(plan.request.kind == ExecutionRequestKind.LifecyclePhase) &&
            assertTrue(plan.request.requestedName == "validate") &&
            assertTrue(plan.steps == Seq(
                PlanStep.ProbeTarget("__.checkFormat"),
                PlanStep.InvokeMill(Seq("__.checkFormat"))
            ))
        },
        test("models explicit goal requests independently from lifecycle phases") {
            val request = ExecutionRequest(
                kind = ExecutionRequestKind.ExplicitGoal,
                requestedName = "inspect-plan",
                repoRoot = Path("/repo"),
                moduleRoot = Path("/repo", "module-b"),
                module = ModuleRef(
                    artifactId = "module-b",
                    packaging = "pom"
                )
            )

            assertTrue(request.kind == ExecutionRequestKind.ExplicitGoal) &&
            assertTrue(request.requestedName == "inspect-plan") &&
            assertTrue(request.module.packaging == "pom")
        },
        test("captures strict failures as first-class plan steps with guidance") {
            val plan = MillExecutionPlan(
                request = ExecutionRequest(
                    kind = ExecutionRequestKind.ExplicitGoal,
                    requestedName = "deploy-site",
                    repoRoot = Path("/repo"),
                    moduleRoot = Path("/repo", "module-c"),
                    module = ModuleRef(
                        artifactId = "module-c",
                        packaging = "jar"
                    )
                ),
                executionMode = ExecutionMode.Strict,
                steps = Seq(
                    PlanStep.Fail(
                        message = "No mapping found for explicit goal 'deploy-site'",
                        guidance = Seq(
                            "Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl",
                            "Run mill resolve __ to inspect available Mill targets"
                        )
                    )
                )
            )

            assertTrue(
                plan.steps == Seq(
                    PlanStep.Fail(
                        message = "No mapping found for explicit goal 'deploy-site'",
                        guidance = Seq(
                            "Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl",
                            "Run mill resolve __ to inspect available Mill targets"
                        )
                    )
                )
            )
        }
    )
