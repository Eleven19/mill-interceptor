package io.eleven19.mill.interceptor

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

object MainSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("Main")(
        suite("CLI routing")(
            test("routes intercept gradle without environment variables") {
                Abort.run[IllegalArgumentException](Cli.parse(Chunk("intercept", "gradle", "build"))).map {
                    case kyo.Result.Success(CliResult.Run(InterceptTool.Gradle, args)) =>
                        assertTrue(args.toList == List("build"))
                    case other =>
                        assertTrue(false)
                }
            },
            test("keeps direct top-level tool commands invalid") {
                Abort.run[IllegalArgumentException](Cli.parse(Chunk("maven", "clean"))).map {
                    case kyo.Result.Error(ex) =>
                        assertTrue(ex.getMessage == "Unsupported command: maven")
                    case other =>
                        assertTrue(false)
                }
            }
        )
    )
