package io.eleven19.mill.interceptor

import kyo.*
import maven.{MavenSetupFormat, MavenSetupOptions}
import shim.{BuildTool, ShimGenerateOptions}

enum InterceptTool derives CanEqual:
    case Maven, Sbt, Gradle

enum CliResult derives CanEqual:
    case Run(tool: InterceptTool, forwardedArgs: Chunk[String])
    case ShimGenerate(options: ShimGenerateOptions)
    case MavenSetup(options: MavenSetupOptions)
    case Help(error: Option[String])

object Cli:

    val usage: String =
        """Usage:
          |  mill-interceptor intercept <maven|mvn|sbt|gradle> [args...]
          |  mill-interceptor maven setup [options]
          |  mill-interceptor shim generate [options]
          |
          |Commands:
          |  intercept       Run the selected tool interceptor
          |  maven setup     Generate Maven plugin setup files
          |  shim generate   Generate platform-specific shim scripts
          |
          |Maven setup options:
          |  --dry-run                 Preview generated files without writing
          |  --format <yaml|pkl>       Starter config format (default: yaml)
          |  --extension-version <v>   Use an explicit Maven extension version
          |  --force                   Overwrite existing generated files
          |
          |Shim generate options:
          |  -t, --tool <tool>         Build tool: maven, gradle, sbt, or all (default: all)
          |  -w, --wrapper             Generate wrapper script names (e.g. mvnw instead of mvn)
          |  -o, --output-dir <dir>    Output directory (default: current directory)
          |  --version <version>       mill-interceptor version for the shim (default: latest)
          |""".stripMargin

    /** Parse CLI arguments into a [[CliResult]].
      *
      * Returns a pure [[CliResult]] suspended in `Abort[IllegalArgumentException]` for invalid input, keeping the
      * parsing itself testable and composable.
      */
    def parse(args: Chunk[String]): CliResult < Abort[IllegalArgumentException] =
        args.toList match
            case Nil                       => CliResult.Help(None)
            case "-h" :: _ | "--help" :: _ => CliResult.Help(None)
            case "intercept" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing intercept tool"))
            case "intercept" :: tool :: rest =>
                tool match
                    case "maven" | "mvn" =>
                        CliResult.Run(InterceptTool.Maven, Chunk.from(rest))
                    case "sbt" =>
                        CliResult.Run(InterceptTool.Sbt, Chunk.from(rest))
                    case "gradle" =>
                        CliResult.Run(InterceptTool.Gradle, Chunk.from(rest))
                    case other =>
                        Abort.fail(new IllegalArgumentException(s"Unsupported intercept tool: $other"))
            case "maven" :: "setup" :: rest =>
                parseMavenSetup(rest)
            case "maven" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing maven subcommand. Use: maven setup"))
            case "maven" :: sub :: _ =>
                Abort.fail(new IllegalArgumentException(s"Unknown maven subcommand: $sub"))
            case "shim" :: "generate" :: rest =>
                parseShimGenerate(rest)
            case "shim" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing shim subcommand. Use: shim generate"))
            case "shim" :: sub :: _ =>
                Abort.fail(new IllegalArgumentException(s"Unknown shim subcommand: $sub"))
            case command :: _ =>
                Abort.fail(new IllegalArgumentException(s"Unsupported command: $command"))

    private def parseMavenSetup(args: List[String]): CliResult < Abort[IllegalArgumentException] =
        parseMavenSetupOpts(args, MavenSetupOptions())

    private def parseMavenSetupOpts(
        args: List[String],
        opts: MavenSetupOptions
    ): CliResult < Abort[IllegalArgumentException] =
        args match
            case Nil =>
                CliResult.MavenSetup(opts)
            case ("-h" | "--help") :: _ =>
                CliResult.Help(None)
            case "--dry-run" :: rest =>
                parseMavenSetupOpts(rest, opts.copy(dryRun = true))
            case "--force" :: rest =>
                parseMavenSetupOpts(rest, opts.copy(force = true))
            case "--extension-version" :: value :: rest =>
                parseMavenSetupOpts(rest, opts.copy(extensionVersion = Some(value)))
            case "--extension-version" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing value for --extension-version"))
            case "--format" :: value :: rest =>
                MavenSetupFormat.fromString(value) match
                    case Some(format) =>
                        parseMavenSetupOpts(rest, opts.copy(format = format))
                    case None =>
                        Abort.fail(
                            new IllegalArgumentException(
                                s"Unsupported format: $value. Use yaml or pkl"
                            )
                        )
            case "--format" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing value for --format"))
            case unknown :: _ =>
                Abort.fail(new IllegalArgumentException(s"Unknown maven setup option: $unknown"))

    private def parseShimGenerate(args: List[String]): CliResult < Abort[IllegalArgumentException] =
        parseShimOpts(args, ShimGenerateOptions.default)

    private def parseShimOpts(
        args: List[String],
        opts: ShimGenerateOptions
    ): CliResult < Abort[IllegalArgumentException] =
        args match
            case Nil =>
                CliResult.ShimGenerate(opts)
            case ("-h" | "--help") :: _ =>
                CliResult.Help(None)
            case ("-t" | "--tool") :: value :: rest =>
                value.toLowerCase match
                    case "all" =>
                        parseShimOpts(rest, opts.copy(tools = BuildTool.all))
                    case other =>
                        BuildTool.fromString(other) match
                            case Some(tool) =>
                                parseShimOpts(rest, opts.copy(tools = List(tool)))
                            case None =>
                                Abort.fail(
                                    new IllegalArgumentException(
                                        s"Unsupported tool: $other. Use maven, gradle, sbt, or all"
                                    )
                                )
            case ("-t" | "--tool") :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing value for --tool"))
            case ("-w" | "--wrapper") :: rest =>
                parseShimOpts(rest, opts.copy(wrapper = true))
            case ("-o" | "--output-dir") :: dir :: rest =>
                parseShimOpts(rest, opts.copy(outputDir = Path(dir)))
            case ("-o" | "--output-dir") :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing value for --output-dir"))
            case "--version" :: v :: rest =>
                parseShimOpts(rest, opts.copy(version = v))
            case "--version" :: Nil =>
                Abort.fail(new IllegalArgumentException("Missing value for --version"))
            case unknown :: _ =>
                Abort.fail(new IllegalArgumentException(s"Unknown shim generate option: $unknown"))
