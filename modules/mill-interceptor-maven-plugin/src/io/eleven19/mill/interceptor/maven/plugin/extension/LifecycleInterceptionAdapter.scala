package io.eleven19.mill.interceptor.maven.plugin.extension

import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.ModuleRef
import io.eleven19.mill.interceptor.maven.plugin.mojo.MavenExecutionContext
import java.io.File
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import kyo.Path
import scala.jdk.CollectionConverters.*

/** Extension-side adapter for deriving lifecycle interception inputs from Maven session state.
  *
  * The core extension should make the lifecycle behavior explicit and testable instead of relying on "bind every phase"
  * behavior. This adapter computes the requested lifecycle phase chain from the Maven goals and translates a project
  * plus phase into the existing neutral execution context.
  */
object LifecycleInterceptionAdapter:

    private val cleanLifecycle = Seq("clean")

    private val defaultLifecycle = Seq(
        "validate",
        "compile",
        "test",
        "package",
        "verify",
        "install",
        "deploy"
    )

    val supportedLifecycleGoals: Seq[String] =
        cleanLifecycle ++ defaultLifecycle

    def requestedLifecyclePhases(session: MavenSession): Seq[String] =
        requestedLifecyclePhases(Option(session.getGoals).map(_.asScala.toSeq).getOrElse(Seq.empty))

    def requestedLifecyclePhases(goals: Seq[String]): Seq[String] =
        val requested        = goals.filter(supportedLifecycleGoals.contains)
        val includeClean     = requested.contains("clean")
        val requestedDefault = requested.filter(defaultLifecycle.contains)
        val expandedDefault = requestedDefault
            .flatMap(goal => defaultLifecycle.takeWhile(_ != goal) :+ goal)
            .distinct

        (if includeClean then cleanLifecycle else Seq.empty) ++ expandedDefault

    def executionContext(session: MavenSession, project: MavenProject, phase: String): MavenExecutionContext =
        val moduleRootFile = Option(project.getBasedir).getOrElse(File("."))
        val repoRootFile =
            Option(session.getExecutionRootDirectory).map(File(_)).getOrElse(moduleRootFile)

        MavenExecutionContext(
            kind = ExecutionRequestKind.LifecyclePhase,
            requestedName = phase,
            repoRoot = Path(repoRootFile.getAbsolutePath),
            moduleRoot = Path(moduleRootFile.getAbsolutePath),
            module = ModuleRef(
                artifactId = Option(project.getArtifactId).getOrElse("unknown-artifact"),
                packaging = Option(project.getPackaging).filter(_.nonEmpty).getOrElse("jar"),
                groupId = Option(project.getGroupId).filter(_.nonEmpty)
            ),
            userProperties = Option(session.getUserProperties)
                .map(
                    _.stringPropertyNames().asScala.iterator
                        .map(name => name -> session.getUserProperties.getProperty(name))
                        .toMap
                )
                .getOrElse(Map.empty)
        )
