package io.github.eleven19.mill.interceptor

import kyo.*
import maven.Mvn
import sbt.Sbt
import gradle.Gradle
import shim.ShimGenerator

object Main extends KyoApp:
    private val scribeLog = ScribeLog("io.github.eleven19.mill.interceptor")

    run {
        Log.let(scribeLog) {
            direct {
                Abort.run[IllegalArgumentException](Cli.parse(args)).now match
                    case Result.Success(CliResult.Run(InterceptTool.Maven, forwardedArgs)) =>
                        Mvn.run(forwardedArgs).now
                    case Result.Success(CliResult.Run(InterceptTool.Sbt, forwardedArgs)) =>
                        Sbt.run(forwardedArgs).now
                    case Result.Success(CliResult.Run(InterceptTool.Gradle, forwardedArgs)) =>
                        Gradle.run(forwardedArgs).now
                    case Result.Success(CliResult.ShimGenerate(options)) =>
                        val generated = ShimGenerator.generate(options).now
                        val _ = Kyo
                            .foreach(generated)(shim => Log.info(s"Generated ${shim.platform} shim: ${shim.path}"))
                            .now
                        Console.printLine(s"Generated ${generated.size} shim script(s)").now
                    case Result.Success(CliResult.Help(None)) =>
                        Console.printLine(Cli.usage).now
                    case Result.Success(CliResult.Help(Some(message))) =>
                        Log.error(message).now
                        Console.printLine(Cli.usage).now
                        Abort.fail(new IllegalArgumentException(message)).now
                    case Result.Error(ex) =>
                        Log.error(ex.getMessage).now
                        Console.printLine(Cli.usage).now
                        Abort.fail(ex).now
                    case Result.Panic(ex) =>
                        Abort.fail(new RuntimeException(s"Unexpected error: ${ex.getMessage}", ex)).now
            }
        }
    }
end Main
