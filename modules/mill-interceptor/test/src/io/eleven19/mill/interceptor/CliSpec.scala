package io.eleven19.mill.interceptor

import zio.test.*
import maven.{MavenSetupFormat, MavenSetupOptions}
import shim.{BuildTool, ShimGenerateOptions}

given CanEqual[os.Path, os.Path] = CanEqual.derived

object CliSpec extends ZIOSpecDefault:

    /** Helper to run parse and get the successful result. */
    private def parseSuccess(args: String*): CliResult =
        Cli.parse(args.toList) match
            case Right(r) => r
            case Left(ex) => throw new AssertionError(s"Expected success but got: $ex")

    /** Helper to run parse and get the error message. */
    private def parseError(args: String*): String =
        Cli.parse(args.toList) match
            case Left(ex)    => ex.getMessage
            case Right(other) => throw new AssertionError(s"Expected error but got: $other")

    def spec: Spec[Any, Any] = suite("Cli")(
        suite("parse - success cases")(
            test("intercept maven forwards remaining args") {
                val result = parseSuccess("intercept", "maven", "clean", "test")
                assertTrue(result match
                    case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("clean", "test")
                    case _                                        => false
                )
            },
            test("intercept mvn accepts the alias") {
                val result = parseSuccess("intercept", "mvn", "install")
                assertTrue(result match
                    case CliResult.Run(InterceptTool.Maven, args) => args.toList == List("install")
                    case _                                        => false
                )
            },
            test("intercept sbt is supported") {
                val result = parseSuccess("intercept", "sbt", "compile")
                assertTrue(result match
                    case CliResult.Run(InterceptTool.Sbt, args) => args.toList == List("compile")
                    case _                                      => false
                )
            },
            test("intercept gradle is supported") {
                val result = parseSuccess("intercept", "gradle", "build")
                assertTrue(result match
                    case CliResult.Run(InterceptTool.Gradle, args) => args.toList == List("build")
                    case _                                         => false
                )
            },
            test("empty args returns help") {
                val result = parseSuccess()
                assertTrue(result == CliResult.Help(None))
            },
            test("-h returns help") {
                val result = parseSuccess("-h")
                assertTrue(result == CliResult.Help(None))
            }
        ),
        suite("parse - error cases")(
            test("intercept without a tool fails with Abort") {
                val msg = parseError("intercept")
                assertTrue(msg == "Missing intercept tool")
            },
            test("unknown top-level command fails with Abort") {
                val msg = parseError("foo", "clean")
                assertTrue(msg == "Unsupported command: foo")
            },
            test("unknown intercept tool fails with Abort") {
                val msg = parseError("intercept", "ant", "compile")
                assertTrue(msg == "Unsupported intercept tool: ant")
            },
            test("shim without subcommand fails with Abort") {
                val msg = parseError("shim")
                assertTrue(msg.contains("Missing shim subcommand"))
            },
            test("shim with unknown subcommand fails with Abort") {
                val msg = parseError("shim", "unknown")
                assertTrue(msg.contains("Unknown shim subcommand"))
            }
        ),
        suite("shim generate - success cases")(
            test("shim generate with no options uses defaults") {
                val result = parseSuccess("shim", "generate")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) =>
                        opts.tools == BuildTool.all &&
                        !opts.wrapper &&
                        opts.version == "latest"
                    case _ => false
                )
            },
            test("shim generate --tool maven selects maven only") {
                val result = parseSuccess("shim", "generate", "--tool", "maven")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Maven)
                    case _                            => false
                )
            },
            test("shim generate -t gradle selects gradle only") {
                val result = parseSuccess("shim", "generate", "-t", "gradle")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Gradle)
                    case _                            => false
                )
            },
            test("shim generate -t sbt selects sbt only") {
                val result = parseSuccess("shim", "generate", "-t", "sbt")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.tools == List(BuildTool.Sbt)
                    case _                            => false
                )
            },
            test("shim generate -t all selects all tools") {
                val result = parseSuccess("shim", "generate", "-t", "all")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.tools == BuildTool.all
                    case _                            => false
                )
            },
            test("shim generate --wrapper enables wrapper mode") {
                val result = parseSuccess("shim", "generate", "--wrapper")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.wrapper
                    case _                            => false
                )
            },
            test("shim generate -w enables wrapper mode") {
                val result = parseSuccess("shim", "generate", "-w")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.wrapper
                    case _                            => false
                )
            },
            test("shim generate --output-dir sets output directory") {
                val result = parseSuccess("shim", "generate", "--output-dir", "/tmp/shims")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) =>
                        opts.outputDir == os.Path(java.nio.file.Paths.get("/tmp/shims"))
                    case _ => false
                )
            },
            test("shim generate -o sets output directory") {
                val result = parseSuccess("shim", "generate", "-o", "/tmp/shims")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) =>
                        opts.outputDir == os.Path(java.nio.file.Paths.get("/tmp/shims"))
                    case _ => false
                )
            },
            test("shim generate --version sets version") {
                val result = parseSuccess("shim", "generate", "--version", "1.2.3")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) => opts.version == "1.2.3"
                    case _                            => false
                )
            },
            test("shim generate with multiple options") {
                val result = parseSuccess("shim", "generate", "-t", "maven", "-w", "-o", "/tmp", "--version", "2.0.0")
                assertTrue(result match
                    case CliResult.ShimGenerate(opts) =>
                        opts.tools == List(BuildTool.Maven) &&
                        opts.wrapper &&
                        opts.outputDir == os.Path(java.nio.file.Paths.get("/tmp")) &&
                        opts.version == "2.0.0"
                    case _ => false
                )
            },
            test("shim generate --help returns help") {
                val result = parseSuccess("shim", "generate", "--help")
                assertTrue(result == CliResult.Help(None))
            }
        ),
        suite("shim generate - error cases")(
            test("shim generate with unsupported tool fails with Abort") {
                val msg = parseError("shim", "generate", "-t", "ant")
                assertTrue(msg.contains("Unsupported tool"))
            },
            test("shim generate --tool without value fails with Abort") {
                val msg = parseError("shim", "generate", "--tool")
                assertTrue(msg == "Missing value for --tool")
            },
            test("shim generate --output-dir without value fails with Abort") {
                val msg = parseError("shim", "generate", "--output-dir")
                assertTrue(msg == "Missing value for --output-dir")
            },
            test("shim generate --version without value fails with Abort") {
                val msg = parseError("shim", "generate", "--version")
                assertTrue(msg == "Missing value for --version")
            }
        ),
        suite("maven setup - success cases")(
            test("maven setup with no options uses defaults") {
                val result = parseSuccess("maven", "setup")
                assertTrue(result == CliResult.MavenSetup(MavenSetupOptions()))
            },
            test("maven setup --dry-run enables dry run") {
                val result = parseSuccess("maven", "setup", "--dry-run")
                assertTrue(result match
                    case CliResult.MavenSetup(opts) => opts.dryRun
                    case _                          => false
                )
            },
            test("maven setup --format yaml selects yaml") {
                val result = parseSuccess("maven", "setup", "--format", "yaml")
                assertTrue(result match
                    case CliResult.MavenSetup(opts) => opts.format == MavenSetupFormat.Yaml
                    case _                          => false
                )
            },
            test("maven setup --format pkl selects pkl") {
                val result = parseSuccess("maven", "setup", "--format", "pkl")
                assertTrue(result match
                    case CliResult.MavenSetup(opts) => opts.format == MavenSetupFormat.Pkl
                    case _                          => false
                )
            },
            test("maven setup --force enables overwrite mode") {
                val result = parseSuccess("maven", "setup", "--force")
                assertTrue(result match
                    case CliResult.MavenSetup(opts) => opts.force
                    case _                          => false
                )
            },
            test("maven setup --extension-version sets the extension version override") {
                val result = parseSuccess("maven", "setup", "--extension-version", "0.3.1")
                assertTrue(result match
                    case CliResult.MavenSetup(opts) => opts.extensionVersion.contains("0.3.1")
                    case _                          => false
                )
            },
            test("maven setup with multiple options") {
                val result = parseSuccess("maven", "setup", "--dry-run", "--format", "pkl", "--force", "--extension-version", "0.3.1")
                assertTrue(result == CliResult.MavenSetup(MavenSetupOptions(dryRun = true, format = MavenSetupFormat.Pkl, force = true, extensionVersion = Some("0.3.1"))))
            },
            test("maven setup --help returns help") {
                val result = parseSuccess("maven", "setup", "--help")
                assertTrue(result == CliResult.Help(None))
            }
        ),
        suite("maven setup - error cases")(
            test("maven setup with unsupported format fails with Abort") {
                val msg = parseError("maven", "setup", "--format", "json")
                assertTrue(msg.contains("Unsupported format"))
            },
            test("maven setup --format without value fails with Abort") {
                val msg = parseError("maven", "setup", "--format")
                assertTrue(msg == "Missing value for --format")
            },
            test("maven setup --extension-version without value fails with Abort") {
                val msg = parseError("maven", "setup", "--extension-version")
                assertTrue(msg == "Missing value for --extension-version")
            },
            test("maven setup with unknown flag fails with Abort") {
                val msg = parseError("maven", "setup", "--bogus")
                assertTrue(msg.contains("Unknown maven setup option"))
            }
        ),
        suite("maven - subcommand error cases")(
            test("maven without subcommand fails with Abort") {
                val msg = parseError("maven")
                assertTrue(msg.contains("Missing maven subcommand"))
            },
            test("maven with unknown subcommand fails with Abort") {
                val msg = parseError("maven", "unknown")
                assertTrue(msg.contains("Unknown maven subcommand"))
            }
        )
    )
