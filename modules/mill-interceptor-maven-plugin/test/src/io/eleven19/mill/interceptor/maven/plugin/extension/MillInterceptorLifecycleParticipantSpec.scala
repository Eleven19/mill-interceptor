package io.eleven19.mill.interceptor.maven.plugin.extension

import io.eleven19.mill.interceptor.maven.plugin.MavenPluginModule
import org.apache.maven.execution.{DefaultMavenExecutionRequest, DefaultMavenExecutionResult, MavenSession}
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import scala.jdk.CollectionConverters.*
import zio.test.*

object MillInterceptorLifecycleParticipantSpec extends ZIOSpecDefault:
    def spec: Spec[Any, Any] = suite("MillInterceptorLifecycleParticipant")(
        test("injects the interceptor plugin for the requested lifecycle chain after project read") {
            val participant = new MillInterceptorLifecycleParticipant
            val project = new MavenProject(Model())
            project.setGroupId("fixture")
            project.setArtifactId("demo")
            project.setVersion("1.0.0")
            project.setPackaging("jar")

            val request = DefaultMavenExecutionRequest()
            request.setGoals(java.util.List.of("compile"))

            val session = MavenSession(
                null,
                request,
                DefaultMavenExecutionResult(),
                java.util.List.of(project)
            )

            participant.afterProjectsRead(session)

            val plugin = project.getBuildPlugins.asScala.find(_.getArtifactId == MavenPluginModule.artifactId)
            val boundPhases = plugin.toSeq.flatMap(_.getExecutions.asScala).map(execution =>
                execution.getPhase -> execution.getGoals.asScala.toSeq
            )

            assertTrue(plugin.nonEmpty) &&
            assertTrue(plugin.exists(_.getGroupId == "io.eleven19.mill-interceptor")) &&
            assertTrue(plugin.exists(_.getVersion == "0.0.0-SNAPSHOT")) &&
            assertTrue(Seq("validate", "compile").forall(goal =>
                boundPhases.exists { case (phase, goals) =>
                    phase == goal && goals == Seq(goal)
                }
            )) &&
            assertTrue(!boundPhases.exists(_._1 == "test")) &&
            assertTrue(!boundPhases.exists(_._1 == "deploy"))
        }
    )
