package io.github.eleven19.mill.interceptor

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*
import shim.{BuildTool, ShimGenerateOptions}
import java.nio.file.Paths

object CliSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("Cli")(
        suite("parse")(
            test("intercept maven forwards remaining args") {
                val parsed = Cli.parse(Chunk("intercept", "maven", "clean", "test"))
                Sync.defer(
                    assertTrue(parsed match
                        case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("clean", "test")
                        case _                                         => false
                    )
                )
            },
            test("intercept mvn accepts the alias") {
                val parsed = Cli.parse(Chunk("intercept", "mvn", "install"))
                Sync.defer(
                    assertTrue(parsed match
                        case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("install")
                        case _                                         => false
                    )
                )
            },
            test("intercept sbt is supported") {
                val parsed = Cli.parse(Chunk("intercept", "sbt", "compile"))
                Sync.defer(
                    assertTrue(parsed match
                        case CliResult.Run(InterceptTool.Sbt, args) => args.toList == List("compile")
                        case _                                      => false
                    )
                )
            },
            test("intercept gradle is supported") {
                val parsed = Cli.parse(Chunk("intercept", "gradle", "build"))
                Sync.defer(
                    assertTrue(parsed match
                        case CliResult.Run(InterceptTool.Gradle, args) => args.toList == List("build")
                        case _                                         => false
                    )
                )
            },
            test("empty args returns help") {
                val parsed = Cli.parse(Chunk.empty)
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(None) => true
                    case _                    => false
                ))
            },
            test("intercept without a tool returns usage error") {
                val parsed = Cli.parse(Chunk("intercept"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Missing intercept tool")) => true
                    case _                                              => false
                ))
            },
            test("unknown top-level command returns help with an error") {
                val parsed = Cli.parse(Chunk("maven", "clean"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Unsupported command: maven")) => true
                    case _                                                   => false
                ))
            },
            test("unknown intercept tool returns help with an error") {
                val parsed = Cli.parse(Chunk("intercept", "ant", "compile"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Unsupported intercept tool: ant")) => true
                    case _                                                        => false
                ))
            }
        ),
        suite("shim generate")(
            test("shim generate with no options uses defaults") {
                val parsed = Cli.parse(Chunk("shim", "generate"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) =>
                        opts.tools == BuildTool.all &&
                        !opts.wrapper &&
                        opts.version == "latest"
                    case _ => false
                ))
            },
            test("shim generate --tool maven selects maven only") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--tool", "maven"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Maven)
                    case _                            => false
                ))
            },
            test("shim generate -t gradle selects gradle only") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-t", "gradle"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Gradle)
                    case _                            => false
                ))
            },
            test("shim generate -t sbt selects sbt only") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-t", "sbt"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Sbt)
                    case _                            => false
                ))
            },
            test("shim generate -t all selects all tools") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-t", "all"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.tools == BuildTool.all
                    case _                            => false
                ))
            },
            test("shim generate --wrapper enables wrapper mode") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--wrapper"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.wrapper
                    case _                            => false
                ))
            },
            test("shim generate -w enables wrapper mode") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-w"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.wrapper
                    case _                            => false
                ))
            },
            test("shim generate --output-dir sets output directory") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--output-dir", "/tmp/shims"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.outputDir.toString == "/tmp/shims"
                    case _                            => false
                ))
            },
            test("shim generate -o sets output directory") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-o", "/tmp/shims"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.outputDir.toString == "/tmp/shims"
                    case _                            => false
                ))
            },
            test("shim generate --version sets version") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--version", "1.2.3"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) => opts.version == "1.2.3"
                    case _                            => false
                ))
            },
            test("shim generate with multiple options") {
                val parsed = Cli.parse(
                    Chunk("shim", "generate", "-t", "maven", "-w", "-o", "/tmp", "--version", "2.0.0")
                )
                Sync.defer(assertTrue(parsed match
                    case CliResult.ShimGenerate(opts) =>
                        opts.tools == List(BuildTool.Maven) &&
                        opts.wrapper &&
                        opts.outputDir.toString == "/tmp" &&
                        opts.version == "2.0.0"
                    case _ => false
                ))
            },
            test("shim generate with unsupported tool returns error") {
                val parsed = Cli.parse(Chunk("shim", "generate", "-t", "ant"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some(msg)) => msg.contains("Unsupported tool")
                    case _                         => false
                ))
            },
            test("shim generate --tool without value returns error") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--tool"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Missing value for --tool")) => true
                    case _                                                => false
                ))
            },
            test("shim generate --output-dir without value returns error") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--output-dir"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Missing value for --output-dir")) => true
                    case _                                                      => false
                ))
            },
            test("shim generate --version without value returns error") {
                val parsed = Cli.parse(Chunk("shim", "generate", "--version"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some("Missing value for --version")) => true
                    case _                                                   => false
                ))
            },
            test("shim without subcommand returns error") {
                val parsed = Cli.parse(Chunk("shim"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some(msg)) => msg.contains("Missing shim subcommand")
                    case _                         => false
                ))
            },
            test("shim with unknown subcommand returns error") {
                val parsed = Cli.parse(Chunk("shim", "unknown"))
                Sync.defer(assertTrue(parsed match
                    case CliResult.Help(Some(msg)) => msg.contains("Unknown shim subcommand")
                    case _                         => false
                ))
            }
        )
    )
