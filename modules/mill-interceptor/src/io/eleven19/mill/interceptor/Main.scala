package io.eleven19.mill.interceptor

import os.Path
import ox.OxApp
import ox.ExitCode
import maven.Mvn
import maven.MavenSetupGenerator
import sbt.Sbt
import gradle.Gradle
import shim.ShimGenerator

object Main extends OxApp:

    def run(args: Vector[String])(using ox.Ox): ExitCode =
        Cli.parse(args.toList) match
            case Left(ex) =>
                System.err.println(ex.getMessage)
                println(Cli.usage)
                ExitCode.Failure(1)
            case Right(CliResult.Run(InterceptTool.Maven, forwardedArgs)) =>
                Mvn.run(forwardedArgs)
                ExitCode.Success
            case Right(CliResult.Run(InterceptTool.Sbt, forwardedArgs)) =>
                Sbt.run(forwardedArgs)
                ExitCode.Success
            case Right(CliResult.Run(InterceptTool.Gradle, forwardedArgs)) =>
                Gradle.run(forwardedArgs)
                ExitCode.Success
            case Right(CliResult.ShimGenerate(options)) =>
                val generated = ShimGenerator.generate(options)
                generated.foreach(shim => scribe.info(s"Generated ${shim.platform} shim: ${shim.path}"))
                println(s"Generated ${generated.size} shim script(s)")
                ExitCode.Success
            case Right(CliResult.MavenSetup(options)) =>
                val extensionVersion = options.extensionVersion.orElse(RuntimeVersion.current) match
                    case Some(version) => version
                    case None =>
                        System.err.println(
                            "Could not determine the mill-interceptor version for .mvn/extensions.xml. Re-run with --extension-version <version>."
                        )
                        return ExitCode.Failure(1)
                MavenSetupGenerator.generate(Path(java.nio.file.Paths.get(".")), options, extensionVersion) match
                    case Left(ex) =>
                        System.err.println(ex.getMessage)
                        ExitCode.Failure(1)
                    case Right(generated) =>
                        val action = if options.dryRun then "Would write" else "Wrote"
                        generated.foreach(file => scribe.info(s"$action ${file.path}"))
                        val message =
                            if options.dryRun then
                                s"Planned ${generated.size} Maven setup file(s). Run mvn mill-interceptor:inspect-plan after applying them."
                            else
                                s"Generated ${generated.size} Maven setup file(s). Next: run mvn mill-interceptor:inspect-plan or mvn validate."
                        println(message)
                        ExitCode.Success
            case Right(CliResult.Help(None)) =>
                println(Cli.usage)
                ExitCode.Success
            case Right(CliResult.Help(Some(message))) =>
                scribe.error(message)
                println(Cli.usage)
                ExitCode.Failure(1)

end Main
