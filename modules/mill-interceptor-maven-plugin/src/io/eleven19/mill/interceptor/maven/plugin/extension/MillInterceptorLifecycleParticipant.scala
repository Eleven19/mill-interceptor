package io.eleven19.mill.interceptor.maven.plugin.extension

import io.eleven19.mill.interceptor.maven.plugin.MavenPluginModule
import javax.inject.Named
import javax.inject.Singleton
import org.apache.maven.AbstractMavenLifecycleParticipant
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.{Build, Plugin, PluginExecution}
import org.apache.maven.project.MavenProject
import scala.jdk.CollectionConverters.*

/** Core-extension bootstrap surface for the Maven interceptor artifact.
  *
  * The first blocker for extension-only activation is making the artifact discoverable by Maven's container when it is
  * loaded from `.mvn/extensions.xml`. The actual lifecycle interception behavior is added in later tasks.
  */
@Named
@Singleton
final class MillInterceptorLifecycleParticipant extends AbstractMavenLifecycleParticipant:

    override def afterProjectsRead(session: MavenSession): Unit =
        val pluginVersion            = MillInterceptorLifecycleParticipant.pluginVersion(getClass.getPackage)
        val requestedLifecyclePhases = LifecycleInterceptionAdapter.requestedLifecyclePhases(session)
        session.getProjects.asScala.foreach(project =>
            MillInterceptorLifecycleParticipant.ensureLifecycleBindings(
                project,
                pluginVersion,
                requestedLifecyclePhases
            )
        )

object MillInterceptorLifecycleParticipant:
    private val pluginGroupId        = "io.eleven19.mill-interceptor"
    private val defaultPluginVersion = "0.0.0-SNAPSHOT"

    private[extension] def pluginVersion(pluginPackage: Package | Null): String =
        Option(pluginPackage)
            .flatMap(pkg => Option(pkg.getImplementationVersion))
            .filter(_.nonEmpty)
            .getOrElse(defaultPluginVersion)

    private[extension] def ensureLifecycleBindings(
        project: MavenProject,
        pluginVersion: String,
        lifecycleGoals: Seq[String]
    ): Unit =
        val build = Option(project.getBuild).getOrElse {
            val created = Build()
            project.setBuild(created)
            created
        }

        val plugin = build.getPlugins.asScala.find(_.getArtifactId == MavenPluginModule.artifactId).getOrElse {
            val created = Plugin()
            created.setGroupId(pluginGroupId)
            created.setArtifactId(MavenPluginModule.artifactId)
            created.setVersion(pluginVersion)
            build.addPlugin(created)
            created
        }

        lifecycleGoals.foreach(goal =>
            if !plugin.getExecutions.asScala.exists(execution => execution.getPhase == goal) then
                val execution = PluginExecution()
                execution.setId(s"mill-interceptor-$goal")
                execution.setPhase(goal)
                execution.addGoal(goal)
                plugin.addExecution(execution)
        )
