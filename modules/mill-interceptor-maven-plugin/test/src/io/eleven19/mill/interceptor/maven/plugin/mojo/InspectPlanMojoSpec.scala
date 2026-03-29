package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.config.MillConfig
import io.eleven19.mill.interceptor.maven.plugin.config.ValidateConfig
import io.eleven19.mill.interceptor.maven.plugin.exec.MillRunner
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerResult
import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.model.ModuleRef
import java.io.File
import os.Path
import org.apache.maven.execution.{DefaultMavenExecutionRequest, DefaultMavenExecutionResult, MavenSession}

given canEqualPath2: CanEqual[os.Path, os.Path] = CanEqual.derived
import org.apache.maven.plugin.logging.Log
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import zio.test.*

object InspectPlanMojoSpec extends ZIOSpecDefault:

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
            assertTrue(log.infoMessages.count(_.contains("cwd: /repo")) == 3)
        },
        test("falls back to the module root when Maven does not provide an execution root directory") {
            val mojo = new DerivedContextInspectPlanMojo
            mojo.configure(
                repoRoot = null,
                moduleRoot = File("/repo/modules/app"),
                artifactId = "app",
                packaging = "jar",
                groupId = "io.eleven19"
            )

            val executionContext = mojo.derivedExecutionContext()

            assertTrue(executionContext.repoRoot == Path("/repo/modules/app")) &&
            assertTrue(executionContext.moduleRoot == Path("/repo/modules/app")) &&
            assertTrue(executionContext.requestedName == "inspect-plan")
        },
        test("uses Maven session and project objects when file parameters are unavailable") {
            val mojo = new DerivedContextInspectPlanMojo
            val project = new MavenProject(Model())
            project.setGroupId("io.eleven19")
            project.setArtifactId("app")
            project.setVersion("1.0.0")
            project.setPackaging("jar")
            project.setFile(File("/repo/modules/app/pom.xml"))

            val request = DefaultMavenExecutionRequest()
            request.setBaseDirectory(File("/repo"))

            val session = MavenSession(
                null,
                request,
                DefaultMavenExecutionResult(),
                java.util.List.of(project)
            )

            mojo.configure(
                repoRoot = null,
                moduleRoot = null,
                artifactId = "",
                packaging = "",
                groupId = ""
            )
            mojo.configureProjectSession(project, session)

            val executionContext = mojo.derivedExecutionContext()

            assertTrue(executionContext.repoRoot == Path("/repo")) &&
            assertTrue(executionContext.moduleRoot == Path("/repo") / "modules" / "app") &&
            assertTrue(executionContext.module.artifactId == "app") &&
            assertTrue(executionContext.module.packaging == "jar") &&
            assertTrue(executionContext.module.groupId.contains("io.eleven19"))
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
            plan: io.eleven19.mill.interceptor.model.MillExecutionPlan,
            config: EffectiveConfig
        ): RunnerResult =
            executedPlan = true
            throw new AssertionError("inspect-plan should not execute subprocesses")

    private final class DerivedContextInspectPlanMojo extends InspectPlanMojo:
        def configure(
            repoRoot: File | Null,
            moduleRoot: File,
            artifactId: String,
            packaging: String,
            groupId: String
        ): Unit =
            this.repoRootDirectory = repoRoot
            this.moduleRootDirectory = moduleRoot
            this.artifactId = artifactId
            this.packaging = packaging
            this.groupId = groupId

        def configureProjectSession(project: MavenProject, session: MavenSession): Unit =
            this.mavenProject = project
            this.mavenSession = session

        def derivedExecutionContext(): MavenExecutionContext =
            executionContext

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
