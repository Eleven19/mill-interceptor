package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.config.MillConfig
import io.eleven19.mill.interceptor.maven.plugin.config.ValidateConfig
import io.eleven19.mill.interceptor.maven.plugin.exec.MillRunner
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerResult
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.ModuleRef
import kyo.Path
import kyo.test.KyoSpecDefault
import org.apache.maven.plugin.logging.Log
import zio.test.*

object InspectPlanMojoSpec extends KyoSpecDefault:

    private def context(kind: ExecutionRequestKind, requestedName: String): MavenExecutionContext =
        MavenExecutionContext(
            kind = kind,
            requestedName = requestedName,
            repoRoot = Path("/repo"),
            moduleRoot = Path("/repo", "modules", "app"),
            module = ModuleRef(
                artifactId = "app",
                packaging = "jar",
                groupId = Some("io.eleven19")
            )
        )

    def spec: Spec[Any, Any] = suite("InspectPlanMojo")(
        test("renders the ordered dry-run plan without executing subprocesses") {
            val log = new RecordingLog
            val mojo = new TestInspectPlanMojo(
                executionContext0 = context(ExecutionRequestKind.LifecyclePhase, "validate"),
                config0 = EffectiveConfig(
                    mill = MillConfig(executable = "millw"),
                    lifecycle = Map("validate" -> Seq("app.validate")),
                    validate = ValidateConfig(
                        scalafmtEnabled = true,
                        scalafmtTarget = Some("__.checkFormat")
                    )
                )
            )

            mojo.setLog(log)
            mojo.execute()

            assertTrue(mojo.executedPlan == false) &&
            assertTrue(log.infoMessages.exists(_.contains("Resolved Mill execution plan"))) &&
            assertTrue(log.infoMessages.exists(_.contains("probe: millw resolve __.checkFormat"))) &&
            assertTrue(log.infoMessages.exists(_.contains("invoke: millw __.checkFormat"))) &&
            assertTrue(log.infoMessages.exists(_.contains("invoke: millw app.validate"))) &&
            assertTrue(log.infoMessages.count(_.contains("cwd: /repo/modules/app")) == 3)
        }
    )

    private final class TestInspectPlanMojo(
        executionContext0: MavenExecutionContext,
        config0: EffectiveConfig
    ) extends InspectPlanMojo:
        var executedPlan = false

        override protected def executionContext: MavenExecutionContext = executionContext0

        override protected def loadEffectiveConfig(): EffectiveConfig = config0

        override protected def executeResolvedPlan(
            plan: io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan,
            config: EffectiveConfig
        ): RunnerResult =
            executedPlan = true
            throw new AssertionError("inspect-plan should not execute subprocesses")

    private final class RecordingLog extends Log:
        val infoMessages = scala.collection.mutable.ArrayBuffer.empty[String]

        override def isDebugEnabled: Boolean = true
        override def isInfoEnabled: Boolean = true
        override def isWarnEnabled: Boolean = true
        override def isErrorEnabled: Boolean = true

        override def debug(content: CharSequence): Unit = ()
        override def debug(content: CharSequence, error: Throwable): Unit = ()
        override def debug(error: Throwable): Unit = ()

        override def info(content: CharSequence): Unit =
            infoMessages.append(content.toString)
        override def info(content: CharSequence, error: Throwable): Unit =
            infoMessages.append(content.toString)
        override def info(error: Throwable): Unit =
            infoMessages.append(error.getMessage)

        override def warn(content: CharSequence): Unit = ()
        override def warn(content: CharSequence, error: Throwable): Unit = ()
        override def warn(error: Throwable): Unit = ()

        override def error(content: CharSequence): Unit = ()
        override def error(content: CharSequence, error: Throwable): Unit = ()
        override def error(error: Throwable): Unit = ()
