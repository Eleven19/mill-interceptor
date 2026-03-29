package io.eleven19.mill.interceptor

import zio.test.*

object MainSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("Main")(
        suite("CLI routing")(
            test("routes intercept gradle without environment variables") {
                Cli.parse(List("intercept", "gradle", "build")) match
                    case Right(CliResult.Run(InterceptTool.Gradle, args)) =>
                        assertTrue(args.toList == List("build"))
                    case other =>
                        assertTrue(false)
            },
            test("rejects unsupported direct maven subcommands") {
                Cli.parse(List("maven", "clean")) match
                    case Left(ex) =>
                        assertTrue(ex.getMessage == "Unknown maven subcommand: clean")
                    case other =>
                        assertTrue(false)
            }
        )
    )
