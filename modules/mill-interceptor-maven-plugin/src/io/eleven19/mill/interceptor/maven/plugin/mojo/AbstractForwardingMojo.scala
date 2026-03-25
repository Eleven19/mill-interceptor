package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.config.ConfigLoadException
import io.eleven19.mill.interceptor.maven.plugin.config.ConfigLoader
import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.exec.DryRunResult
import io.eleven19.mill.interceptor.maven.plugin.exec.DryRunStep
import io.eleven19.mill.interceptor.maven.plugin.exec.MillRunner
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerFailure
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerResult
import io.eleven19.mill.interceptor.maven.plugin.exec.RunnerStepKind
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import io.eleven19.mill.interceptor.maven.plugin.model.MillExecutionPlan
import io.eleven19.mill.interceptor.maven.plugin.model.ModuleRef
import io.eleven19.mill.interceptor.maven.plugin.resolve.ExecutionPlanResolver
import java.io.File
import java.util.Properties
import kyo.*
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

/** Shared Mojo base for lifecycle-forwarding Maven goals.
  *
  * Concrete lifecycle and operational Mojos only need to identify the request they represent. This base class handles
  * config loading, plan resolution, execution, inspect-plan rendering, and failure translation.
  */
abstract class AbstractForwardingMojo extends AbstractMojo:

    @Parameter(defaultValue = "${session.executionRootDirectory}", readonly = true, required = true)
    protected var repoRootDirectory: File = uninitialized

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected var moduleRootDirectory: File = uninitialized

    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    protected var artifactId: String = uninitialized

    @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
    protected var packaging: String = uninitialized

    @Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
    protected var groupId: String = uninitialized

    @Parameter(defaultValue = "${session.userProperties}", readonly = true)
    protected var sessionUserProperties: Properties = Properties()

    protected def executionKind: ExecutionRequestKind

    protected def requestedName: String

    protected def inspectOnly: Boolean = false

    private def normalizedPath(file: File | Null, fallback: => File): Path =
        Path(Option(file).getOrElse(fallback).getAbsolutePath)

    protected def executionContext: MavenExecutionContext =
        val moduleRootFile = Option(moduleRootDirectory).getOrElse(File("."))
        val repoRootFile   = Option(repoRootDirectory).getOrElse(moduleRootFile)
        MavenExecutionContext(
            kind = executionKind,
            requestedName = requestedName,
            repoRoot = normalizedPath(repoRootFile, moduleRootFile),
            moduleRoot = normalizedPath(moduleRootFile, repoRootFile),
            module = ModuleRef(
                artifactId = artifactId,
                packaging = packaging,
                groupId = Option(groupId).filter(_.nonEmpty)
            ),
            userProperties = sessionUserProperties
                .stringPropertyNames()
                .asScala
                .iterator
                .map { name =>
                    name -> sessionUserProperties.getProperty(name)
                }
                .toMap
        )

    final protected def executionRequest: ExecutionRequest =
        executionContext.toExecutionRequest

    protected def loadEffectiveConfig(): EffectiveConfig =
        given AllowUnsafe = AllowUnsafe.embrace.danger
        Sync.Unsafe
            .evalOrThrow(
                Abort.run[ConfigLoadException](
                    ConfigLoader.load(executionContext.repoRoot, executionContext.moduleRoot)
                )
            ) match
            case Result.Success(config) => config
            case Result.Error(error) =>
                throw MojoExecutionException(error.getMessage, error)
            case Result.Panic(error) =>
                throw MojoExecutionException(s"Unexpected config load failure: ${error.getMessage}", error)

    protected def resolvePlan(config: EffectiveConfig): MillExecutionPlan =
        ExecutionPlanResolver.resolve(executionRequest, config)

    protected def executeResolvedPlan(plan: MillExecutionPlan, config: EffectiveConfig): RunnerResult =
        given AllowUnsafe = AllowUnsafe.embrace.danger
        Sync.Unsafe.evalOrThrow(MillRunner.execute(plan, config))

    protected def inspectResolvedPlan(plan: MillExecutionPlan, config: EffectiveConfig): DryRunResult =
        MillRunner.dryRun(plan, config)

    final override def execute(): Unit =
        val config = loadEffectiveConfig()
        val plan   = resolvePlan(config)

        if inspectOnly then renderInspectPlan(inspectResolvedPlan(plan, config))
        else
            executeResolvedPlan(plan, config) match
                case _: RunnerResult.Success =>
                    ()
                case RunnerResult.Failure(_, failure) =>
                    throw mapFailure(failure)

    private def renderInspectPlan(result: DryRunResult): Unit =
        getLog.info(s"Resolved Mill execution plan for ${executionRequest.requestedName}:")
        result.steps.zipWithIndex.foreach { case (step, index) =>
            renderDryRunStep(index + 1, step).foreach(getLog.info)
        }

    private def renderDryRunStep(index: Int, step: DryRunStep): Seq[String] =
        val prefix = s"[$index]"
        step.kind match
            case RunnerStepKind.ProbeTarget | RunnerStepKind.InvokeMill =>
                val kind = step.kind match
                    case RunnerStepKind.ProbeTarget => "probe"
                    case RunnerStepKind.InvokeMill  => "invoke"
                    case RunnerStepKind.Fail        => "fail"
                Seq(
                    s"$prefix $kind: ${step.command.getOrElse(Seq.empty).mkString(" ")}",
                    s"$prefix cwd: ${step.workingDirectory.toJava}"
                )
            case RunnerStepKind.Fail =>
                Seq(s"$prefix fail: ${step.message.getOrElse("Execution plan failed")}") ++
                    step.guidance.map(guidance => s"$prefix guidance: $guidance")

    private def mapFailure(failure: RunnerFailure): Exception =
        failure match
            case RunnerFailure.FailStep(message, guidance) =>
                MojoFailureException(renderDiagnostic(message, guidance = guidance))
            case RunnerFailure.ProbeFailure(_, command, exitCode, message, guidance) =>
                MojoFailureException(
                    renderDiagnostic(
                        message,
                        command = Some(command),
                        exitCode = exitCode,
                        guidance = guidance
                    )
                )
            case RunnerFailure.InvocationFailure(command, exitCode, message, guidance) =>
                MojoExecutionException(
                    renderDiagnostic(
                        message,
                        command = Some(command),
                        exitCode = exitCode,
                        guidance = guidance
                    )
                )

    private def renderDiagnostic(
        message: String,
        command: Option[Seq[String]] = None,
        exitCode: Option[Int] = None,
        guidance: Seq[String] = Seq.empty
    ): String =
        val details =
            command.map(cmd => s"Command: ${cmd.mkString(" ")}").toSeq ++
                exitCode.map(code => s"Exit code: $code").toSeq ++
                guidance
        (message +: details).mkString("\n")
