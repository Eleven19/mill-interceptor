package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerFailure
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerResult
import io.eleven19.mill.interceptor.model.*
import kyo.Path
import kyo.test.KyoSpecDefault
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import scala.util.Try
import zio.test.*

object MojoFailureMappingSpec extends KyoSpecDefault:

    private val executionContext = MavenExecutionContext(
        kind = ExecutionRequestKind.LifecyclePhase,
        requestedName = "compile",
        repoRoot = Path("/repo"),
        moduleRoot = Path("/repo", "modules", "app"),
        module = ModuleRef(
            artifactId = "app",
            packaging = "jar",
            groupId = Some("io.eleven19")
        )
    )

    private val plan = MillExecutionPlan(
        request = executionContext.toExecutionRequest,
        executionMode = ExecutionMode.Strict,
        steps = Seq(PlanStep.InvokeMill(Seq("app.compile")))
    )

    def spec: Spec[Any, Any] = suite("MojoFailureMapping")(
        test("maps fail-step failures to MojoFailureException and preserves guidance") {
            val mojo = new TestForwardingMojo(
                executionContext0 = executionContext,
                result0 = RunnerResult.Failure(
                    stepResults = Seq.empty,
                    failure = RunnerFailure.FailStep(
                        message = "No mapping found for lifecycle phase 'compile'",
                        guidance = Seq("Add a lifecycle mapping in mill-interceptor.yaml or mill-interceptor.pkl")
                    )
                )
            )

            val thrown = Try(mojo.execute()).failed.toOption.collect { case ex: MojoFailureException => ex }

            assertTrue(thrown.nonEmpty) &&
            assertTrue(thrown.exists(_.getMessage.contains("No mapping found for lifecycle phase 'compile'"))) &&
            assertTrue(thrown.exists(_.getMessage.contains("Add a lifecycle mapping in mill-interceptor.yaml or mill-interceptor.pkl")))
        },
        test("maps probe failures to MojoFailureException and preserves guidance") {
            val mojo = new TestForwardingMojo(
                executionContext0 = executionContext,
                result0 = RunnerResult.Failure(
                    stepResults = Seq.empty,
                    failure = RunnerFailure.ProbeFailure(
                        target = "__.checkFormat",
                        command = Seq("millw", "resolve", "__.checkFormat"),
                        exitCode = Some(17),
                        message = "Mill target '__.checkFormat' is unavailable",
                        guidance = Seq("Run `mill resolve __.checkFormat` to inspect available targets")
                    )
                )
            )

            val thrown = Try(mojo.execute()).failed.toOption.collect { case ex: MojoFailureException => ex }

            assertTrue(thrown.nonEmpty) &&
            assertTrue(thrown.exists(_.getMessage.contains("Mill target '__.checkFormat' is unavailable"))) &&
            assertTrue(thrown.exists(_.getMessage.contains("millw resolve __.checkFormat"))) &&
            assertTrue(thrown.exists(_.getMessage.contains("Run `mill resolve __.checkFormat` to inspect available targets")))
        },
        test("maps invocation failures to MojoExecutionException and preserves command context") {
            val mojo = new TestForwardingMojo(
                executionContext0 = executionContext,
                result0 = RunnerResult.Failure(
                    stepResults = Seq.empty,
                    failure = RunnerFailure.InvocationFailure(
                        command = Seq("millw", "app.compile"),
                        exitCode = Some(9),
                        message = "Mill exited with code 9"
                    )
                )
            )

            val thrown = Try(mojo.execute()).failed.toOption.collect { case ex: MojoExecutionException => ex }

            assertTrue(thrown.nonEmpty) &&
            assertTrue(thrown.exists(_.getMessage.contains("Mill exited with code 9"))) &&
            assertTrue(thrown.exists(_.getMessage.contains("millw app.compile")))
        }
    )

    private final class TestForwardingMojo(
        executionContext0: MavenExecutionContext,
        result0: RunnerResult
    ) extends AbstractForwardingMojo:
        override protected def executionKind: ExecutionRequestKind = executionContext0.kind

        override protected def requestedName: String = executionContext0.requestedName

        override protected def executionContext: MavenExecutionContext = executionContext0

        override protected def loadEffectiveConfig(): EffectiveConfig = EffectiveConfig()

        override protected def resolvePlan(config: EffectiveConfig): MillExecutionPlan = plan

        override protected def executeResolvedPlan(plan: MillExecutionPlan, config: EffectiveConfig): RunnerResult =
            result0
