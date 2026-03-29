package io.eleven19.mill.interceptor

import os.Path
import maven.{MavenSetupFormat, MavenSetupOptions}
import shim.{BuildTool, ShimGenerateOptions}

enum InterceptTool derives CanEqual:
    case Maven, Sbt, Gradle

enum CliResult derives CanEqual:
    case Run(tool: InterceptTool, forwardedArgs: Seq[String])
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
      * Returns a pure [[CliResult]] or a `Left[IllegalArgumentException]` for invalid input, keeping the
      * parsing itself testable and composable.
      */
    def parse(args: List[String]): Either[IllegalArgumentException, CliResult] =
        args match
            case Nil                       => Right(CliResult.Help(None))
            case "-h" :: _ | "--help" :: _ => Right(CliResult.Help(None))
            case "intercept" :: Nil =>
                Left(new IllegalArgumentException("Missing intercept tool"))
            case "intercept" :: tool :: rest =>
                tool match
                    case "maven" | "mvn" =>
                        Right(CliResult.Run(InterceptTool.Maven, rest))
                    case "sbt" =>
                        Right(CliResult.Run(InterceptTool.Sbt, rest))
                    case "gradle" =>
                        Right(CliResult.Run(InterceptTool.Gradle, rest))
                    case other =>
                        Left(new IllegalArgumentException(s"Unsupported intercept tool: $other"))
            case "maven" :: "setup" :: rest =>
                parseMavenSetup(rest)
            case "maven" :: Nil =>
                Left(new IllegalArgumentException("Missing maven subcommand. Use: maven setup"))
            case "maven" :: sub :: _ =>
                Left(new IllegalArgumentException(s"Unknown maven subcommand: $sub"))
            case "shim" :: "generate" :: rest =>
                parseShimGenerate(rest)
            case "shim" :: Nil =>
                Left(new IllegalArgumentException("Missing shim subcommand. Use: shim generate"))
            case "shim" :: sub :: _ =>
                Left(new IllegalArgumentException(s"Unknown shim subcommand: $sub"))
            case command :: _ =>
                Left(new IllegalArgumentException(s"Unsupported command: $command"))

    private def parseMavenSetup(args: List[String]): Either[IllegalArgumentException, CliResult] =
        parseMavenSetupOpts(args, MavenSetupOptions())

    private def parseMavenSetupOpts(
        args: List[String],
        opts: MavenSetupOptions
    ): Either[IllegalArgumentException, CliResult] =
        args match
            case Nil =>
                Right(CliResult.MavenSetup(opts))
            case ("-h" | "--help") :: _ =>
                Right(CliResult.Help(None))
            case "--dry-run" :: rest =>
                parseMavenSetupOpts(rest, opts.copy(dryRun = true))
            case "--force" :: rest =>
                parseMavenSetupOpts(rest, opts.copy(force = true))
            case "--extension-version" :: value :: rest =>
                parseMavenSetupOpts(rest, opts.copy(extensionVersion = Some(value)))
            case "--extension-version" :: Nil =>
                Left(new IllegalArgumentException("Missing value for --extension-version"))
            case "--format" :: value :: rest =>
                MavenSetupFormat.fromString(value) match
                    case Some(format) =>
                        parseMavenSetupOpts(rest, opts.copy(format = format))
                    case None =>
                        Left(
                            new IllegalArgumentException(
                                s"Unsupported format: $value. Use yaml or pkl"
                            )
                        )
            case "--format" :: Nil =>
                Left(new IllegalArgumentException("Missing value for --format"))
            case unknown :: _ =>
                Left(new IllegalArgumentException(s"Unknown maven setup option: $unknown"))

    private def parseShimGenerate(args: List[String]): Either[IllegalArgumentException, CliResult] =
        parseShimOpts(args, ShimGenerateOptions.default)

    private def parseShimOpts(
        args: List[String],
        opts: ShimGenerateOptions
    ): Either[IllegalArgumentException, CliResult] =
        args match
            case Nil =>
                Right(CliResult.ShimGenerate(opts))
            case ("-h" | "--help") :: _ =>
                Right(CliResult.Help(None))
            case ("-t" | "--tool") :: value :: rest =>
                value.toLowerCase match
                    case "all" =>
                        parseShimOpts(rest, opts.copy(tools = BuildTool.all))
                    case other =>
                        BuildTool.fromString(other) match
                            case Some(tool) =>
                                parseShimOpts(rest, opts.copy(tools = List(tool)))
                            case None =>
                                Left(
                                    new IllegalArgumentException(
                                        s"Unsupported tool: $other. Use maven, gradle, sbt, or all"
                                    )
                                )
            case ("-t" | "--tool") :: Nil =>
                Left(new IllegalArgumentException("Missing value for --tool"))
            case ("-w" | "--wrapper") :: rest =>
                parseShimOpts(rest, opts.copy(wrapper = true))
            case ("-o" | "--output-dir") :: dir :: rest =>
                parseShimOpts(rest, opts.copy(outputDir = Path(java.nio.file.Paths.get(dir))))
            case ("-o" | "--output-dir") :: Nil =>
                Left(new IllegalArgumentException("Missing value for --output-dir"))
            case "--version" :: v :: rest =>
                parseShimOpts(rest, opts.copy(version = v))
            case "--version" :: Nil =>
                Left(new IllegalArgumentException("Missing value for --version"))
            case unknown :: _ =>
                Left(new IllegalArgumentException(s"Unknown shim generate option: $unknown"))
