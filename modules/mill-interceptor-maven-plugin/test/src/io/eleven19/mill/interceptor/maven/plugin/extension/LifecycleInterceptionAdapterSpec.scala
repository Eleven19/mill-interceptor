package io.eleven19.mill.interceptor.maven.plugin.extension

import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.model.ModuleRef
import kyo.Path
import kyo.test.KyoSpecDefault
import org.apache.maven.execution.{DefaultMavenExecutionRequest, DefaultMavenExecutionResult, MavenSession}
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import zio.test.*

import java.io.File
import java.util.Properties

object LifecycleInterceptionAdapterSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("LifecycleInterceptionAdapter")(
        test("expands the default lifecycle chain up to the requested phase") {
            val phases = LifecycleInterceptionAdapter.requestedLifecyclePhases(Seq("compile"))

            assertTrue(phases == Seq("validate", "compile"))
        },
        test("keeps clean independent from the default lifecycle chain") {
            val phases = LifecycleInterceptionAdapter.requestedLifecyclePhases(Seq("clean", "package"))

            assertTrue(phases == Seq("clean", "validate", "compile", "test", "package"))
        },
        test("ignores explicit plugin goals when deriving lifecycle phases") {
            val phases = LifecycleInterceptionAdapter.requestedLifecyclePhases(
                Seq("mill-interceptor:inspect-plan", "test")
            )

            assertTrue(phases == Seq("validate", "compile", "test"))
        },
        test("builds a lifecycle execution context from Maven session state") {
            val project = new MavenProject(Model())
            project.setGroupId("io.eleven19")
            project.setArtifactId("demo")
            project.setVersion("1.0.0")
            project.setPackaging("jar")
            project.setFile(File("/repo/modules/demo/pom.xml"))

            val request = DefaultMavenExecutionRequest()
            request.setGoals(java.util.List.of("compile"))
            request.setBaseDirectory(File("/repo"))

            val userProperties = Properties()
            userProperties.setProperty("skipTests", "true")
            request.setUserProperties(userProperties)

            val session = MavenSession(
                null,
                request,
                DefaultMavenExecutionResult(),
                java.util.List.of(project)
            )

            val context = LifecycleInterceptionAdapter.executionContext(session, project, "compile")

            assertTrue(context.kind == ExecutionRequestKind.LifecyclePhase) &&
            assertTrue(context.requestedName == "compile") &&
            assertTrue(context.repoRoot == Path("/repo")) &&
            assertTrue(context.moduleRoot == Path("/repo/modules/demo")) &&
            assertTrue(
                context.module == ModuleRef(
                    artifactId = "demo",
                    packaging = "jar",
                    groupId = Some("io.eleven19")
                )
            ) &&
            assertTrue(context.userProperties == Map("skipTests" -> "true"))
        }
    )
