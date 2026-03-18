package io.eleven19.mill.interceptor.maven.plugin

import scala.util.Using

object MavenPluginModule:
    final case class GoalDescriptor(
        goal: String,
        implementationClass: String,
        description: String
    )

    val artifactId = "mill-interceptor-maven-plugin"
    val goalPrefix = "mill-interceptor"
    val placeholderMessage = "Mill Interceptor Maven plugin placeholder goal invoked."

    private val supportedGoalsResource = "/maven-plugin-goals.tsv"

    private def parseGoalDescriptor(line: String): GoalDescriptor =
        line.split('\t').toList match
            case goal :: implementationClass :: description :: Nil =>
                GoalDescriptor(goal, implementationClass, description)
            case _ =>
                throw new IllegalStateException(s"Invalid Maven plugin goal registry line: $line")

    val supportedGoals: Seq[GoalDescriptor] =
        Option(getClass.getResourceAsStream(supportedGoalsResource))
            .map { stream =>
                Using.resource(scala.io.Source.fromInputStream(stream))(_.getLines().toSeq)
            }
            .getOrElse {
                throw new IllegalStateException(
                  s"Unable to load Maven plugin goal registry resource: $supportedGoalsResource"
                )
            }
            .map(_.trim)
            .filter(line => line.nonEmpty && !line.startsWith("#"))
            .map(parseGoalDescriptor)
