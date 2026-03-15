package io.github.eleven19.mill.interceptor

import caseapp.*
import kyo.*
import maven.Mvn
import sbt.Sbt
import gradle.Gradle

object Main extends KyoApp:
    private val scribeLog = ScribeLog("io.github.eleven19.mill.interceptor")

    run {
        Log.let(scribeLog) {
            direct {
                val interceptedBuildToolFromEnv =
                    System.env[String]("INTERCEPTED_BUILD_TOOL").now

                process(args, interceptedBuildToolFromEnv).now
            }
        }
    }

    def process(args: Chunk[String], interceptedBuildTool: Maybe[String]) = direct {
        interceptedBuildTool match
            case Absent =>
                Log.error("No intercepted build tool found").now
                Abort.fail(new IllegalStateException("No intercepted build tool found")).now
            case Present("maven") | Present("mvn") =>
                Mvn.run(args).now
            case Present("sbt") =>
                Sbt.run(args).now
            case Present("gradle") =>
                Gradle.run(args).now
            case Present(buildTool) =>
                Log.error(s"Unsupported build tool: $buildTool").now
                Abort.fail(new UnsupportedOperationException(s"Unsupported build tool: $buildTool")).now
    }
end Main
