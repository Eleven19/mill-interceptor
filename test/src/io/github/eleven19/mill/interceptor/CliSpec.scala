package io.github.eleven19.mill.interceptor

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*
import shim.{BuildTool, ShimGenerateOptions}
import java.nio.file.Paths

object CliSpec extends KyoSpecDefault:

    /** Helper to run parse and get the successful result. */
    private def parseSuccess(args: String*): CliResult < Any =
        Abort.run[IllegalArgumentException](Cli.parse(Chunk.from(args))).map {
            case kyo.Result.Success(r) => r
            case other             => throw new AssertionError(s"Expected success but got: $other")
        }

    /** Helper to run parse and get the error message. */
    private def parseError(args: String*): String < Any =
        Abort.run[IllegalArgumentException](Cli.parse(Chunk.from(args))).map {
            case kyo.Result.Error(ex) => ex.getMessage
            case other            => throw new AssertionError(s"Expected error but got: $other")
        }

    def spec: Spec[Any, Any] = suite("Cli")(
        suite("parse - success cases")(
            test("intercept maven forwards remaining args") {
                parseSuccess("intercept", "maven", "clean", "test").map { result =>
                    assertTrue(result match
                        case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("clean", "test")
                        case _                                        => false
                    )
                }
            },
            test("intercept mvn accepts the alias") {
                parseSuccess("intercept", "mvn", "install").map { result =>
                    assertTrue(result match
                        case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("install")
                        case _                                        => false
                    )
                }
            },
            test("intercept sbt is supported") {
                parseSuccess("intercept", "sbt", "compile").map { result =>
                    assertTrue(result match
                        case CliResult.Run(InterceptTool.Sbt, args) => args.toList == List("compile")
                        case _                                      => false
                    )
                }
            },
            test("intercept gradle is supported") {
                parseSuccess("intercept", "gradle", "build").map { result =>
                    assertTrue(result match
                        case CliResult.Run(InterceptTool.Gradle, args) => args.toList == List("build")
                        case _                                         => false
                    )
                }
            },
            test("empty args returns help") {
                parseSuccess().map { result =>
                    assertTrue(result == CliResult.Help(None))
                }
            },
            test("-h returns help") {
                parseSuccess("-h").map { result =>
                    assertTrue(result == CliResult.Help(None))
                }
            }
        ),
        suite("parse - error cases")(
            test("intercept without a tool fails with Abort") {
                parseError("intercept").map { msg =>
                    assertTrue(msg == "Missing intercept tool")
                }
            },
            test("unknown top-level command fails with Abort") {
                parseError("maven", "clean").map { msg =>
                    assertTrue(msg == "Unsupported command: maven")
                }
            },
            test("unknown intercept tool fails with Abort") {
                parseError("intercept", "ant", "compile").map { msg =>
                    assertTrue(msg == "Unsupported intercept tool: ant")
                }
            },
            test("shim without subcommand fails with Abort") {
                parseError("shim").map { msg =>
                    assertTrue(msg.contains("Missing shim subcommand"))
                }
            },
            test("shim with unknown subcommand fails with Abort") {
                parseError("shim", "unknown").map { msg =>
                    assertTrue(msg.contains("Unknown shim subcommand"))
                }
            }
        ),
        suite("shim generate - success cases")(
            test("shim generate with no options uses defaults") {
                parseSuccess("shim", "generate").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) =>
                            opts.tools == BuildTool.all &&
                            !opts.wrapper &&
                            opts.version == "latest"
                        case _ => false
                    )
                }
            },
            test("shim generate --tool maven selects maven only") {
                parseSuccess("shim", "generate", "--tool", "maven").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Maven)
                        case _                            => false
                    )
                }
            },
            test("shim generate -t gradle selects gradle only") {
                parseSuccess("shim", "generate", "-t", "gradle").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Gradle)
                        case _                            => false
                    )
                }
            },
            test("shim generate -t sbt selects sbt only") {
                parseSuccess("shim", "generate", "-t", "sbt").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Sbt)
                        case _                            => false
                    )
                }
            },
            test("shim generate -t all selects all tools") {
                parseSuccess("shim", "generate", "-t", "all").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.tools == BuildTool.all
                        case _                            => false
                    )
                }
            },
            test("shim generate --wrapper enables wrapper mode") {
                parseSuccess("shim", "generate", "--wrapper").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.wrapper
                        case _                            => false
                    )
                }
            },
            test("shim generate -w enables wrapper mode") {
                parseSuccess("shim", "generate", "-w").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.wrapper
                        case _                            => false
                    )
                }
            },
            test("shim generate --output-dir sets output directory") {
                parseSuccess("shim", "generate", "--output-dir", "/tmp/shims").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.outputDir.toString == "/tmp/shims"
                        case _                            => false
                    )
                }
            },
            test("shim generate -o sets output directory") {
                parseSuccess("shim", "generate", "-o", "/tmp/shims").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.outputDir.toString == "/tmp/shims"
                        case _                            => false
                    )
                }
            },
            test("shim generate --version sets version") {
                parseSuccess("shim", "generate", "--version", "1.2.3").map { result =>
                    assertTrue(result match
                        case CliResult.ShimGenerate(opts) => opts.version == "1.2.3"
                        case _                            => false
                    )
                }
            },
            test("shim generate with multiple options") {
                parseSuccess("shim", "generate", "-t", "maven", "-w", "-o", "/tmp", "--version", "2.0.0").map {
                    result =>
                        assertTrue(result match
                            case CliResult.ShimGenerate(opts) =>
                                opts.tools == List(BuildTool.Maven) &&
                                opts.wrapper &&
                                opts.outputDir.toString == "/tmp" &&
                                opts.version == "2.0.0"
                            case _ => false
                        )
                }
            },
            test("shim generate --help returns help") {
                parseSuccess("shim", "generate", "--help").map { result =>
                    assertTrue(result == CliResult.Help(None))
                }
            }
        ),
        suite("shim generate - error cases")(
            test("shim generate with unsupported tool fails with Abort") {
                parseError("shim", "generate", "-t", "ant").map { msg =>
                    assertTrue(msg.contains("Unsupported tool"))
                }
            },
            test("shim generate --tool without value fails with Abort") {
                parseError("shim", "generate", "--tool").map { msg =>
                    assertTrue(msg == "Missing value for --tool")
                }
            },
            test("shim generate --output-dir without value fails with Abort") {
                parseError("shim", "generate", "--output-dir").map { msg =>
                    assertTrue(msg == "Missing value for --output-dir")
                }
            },
            test("shim generate --version without value fails with Abort") {
                parseError("shim", "generate", "--version").map { msg =>
                    assertTrue(msg == "Missing value for --version")
                }
            }
        )
    )
