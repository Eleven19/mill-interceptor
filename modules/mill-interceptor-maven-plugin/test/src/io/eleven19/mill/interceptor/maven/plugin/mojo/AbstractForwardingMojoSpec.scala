package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.*
import os.Path
import zio.test.*

given canEqualPath1: CanEqual[os.Path, os.Path] = CanEqual.derived

object AbstractForwardingMojoSpec extends ZIOSpecDefault:

    private def context(kind: ExecutionRequestKind, requestedName: String): MavenExecutionContext =
        MavenExecutionContext(
            kind = kind,
            requestedName = requestedName,
            repoRoot = Path("/repo"),
            moduleRoot = Path("/repo") / "modules" / "app",
            module = ModuleRef(
                artifactId = "app",
                packaging = "jar",
                groupId = Some("io.eleven19")
            ),
            userProperties = Map(
                "skipTests" -> "true",
                "profile" -> "ci"
            )
        )

    def spec: Spec[Any, Any] = suite("AbstractForwardingMojo")(
        test("builds a neutral execution request from Maven-like context") {
            val executionContext = context(ExecutionRequestKind.LifecyclePhase, "validate")
            val mojo = new TestForwardingMojo(executionContext)

            assertTrue(
                mojo.request == ExecutionRequest(
                    kind = ExecutionRequestKind.LifecyclePhase,
                    requestedName = "validate",
                    repoRoot = Path("/repo"),
                    moduleRoot = Path("/repo") / "modules" / "app",
                    module = ModuleRef(
                        artifactId = "app",
                        packaging = "jar",
                        groupId = Some("io.eleven19")
                    ),
                    properties = Map(
                        "skipTests" -> "true",
                        "profile" -> "ci"
                    )
                )
            )
        },
        test("distinguishes lifecycle phases from explicit goals") {
            val lifecycleContext = context(ExecutionRequestKind.LifecyclePhase, "compile")
            val goalContext = context(ExecutionRequestKind.ExplicitGoal, "inspect-plan")

            assertTrue(lifecycleContext.isLifecyclePhase) &&
            assertTrue(!lifecycleContext.isExplicitGoal) &&
            assertTrue(goalContext.isExplicitGoal) &&
            assertTrue(!goalContext.isLifecyclePhase) &&
            assertTrue(
                lifecycleContext.toExecutionRequest.kind == ExecutionRequestKind.LifecyclePhase
            ) &&
            assertTrue(
                goalContext.toExecutionRequest.kind == ExecutionRequestKind.ExplicitGoal
            )
        },
        test("preserves repo root, module root, module identity, and user properties") {
            val executionContext = context(ExecutionRequestKind.ExplicitGoal, "describe")

            assertTrue(executionContext.toExecutionRequest.repoRoot == Path("/repo")) &&
            assertTrue(executionContext.toExecutionRequest.moduleRoot == Path("/repo") / "modules" / "app") &&
            assertTrue(executionContext.toExecutionRequest.module.artifactId == "app") &&
            assertTrue(executionContext.toExecutionRequest.module.packaging == "jar") &&
            assertTrue(executionContext.toExecutionRequest.module.groupId.contains("io.eleven19")) &&
            assertTrue(executionContext.toExecutionRequest.properties == Map(
                "skipTests" -> "true",
                "profile" -> "ci"
            ))
        }
    )

    private final class TestForwardingMojo(context: MavenExecutionContext) extends AbstractForwardingMojo:
        override protected def executionKind: ExecutionRequestKind = context.kind

        override protected def requestedName: String = context.requestedName

        override protected def executionContext: MavenExecutionContext = context

        def request: ExecutionRequest =
            executionRequest
