package io.github.eleven19.mill.interceptor

import kyo.*
import shim.{BuildTool, ShimGenerateOptions}
import java.nio.file.Paths

enum InterceptTool derives CanEqual:
    case Maven, Sbt, Gradle

enum CliResult derives CanEqual:
    case Run(tool: InterceptTool, forwardedArgs: Chunk[String])
    case ShimGenerate(options: ShimGenerateOptions)
    case Help(error: Option[String])

object Cli:

    val usage: String =
        """Usage:
          |  mill-interceptor intercept <maven|mvn|sbt|gradle> [args...]
          |  mill-interceptor shim generate [options]
          |
          |Commands:
          |  intercept       Run the selected tool interceptor
          |  shim generate   Generate platform-specific shim scripts
          |
          |Shim generate options:
          |  -t, --tool <tool>         Build tool: maven, gradle, sbt, or all (default: all)
          |  -w, --wrapper             Generate wrapper script names (e.g. mvnw instead of mvn)
          |  -o, --output-dir <dir>    Output directory (default: current directory)
          |  --version <version>       mill-interceptor version for the shim (default: latest)
          |""".stripMargin

    def parse(args: Chunk[String]): CliResult =
        args.toList match
            case Nil                       => CliResult.Help(None)
            case "-h" :: _ | "--help" :: _ => CliResult.Help(None)
            case "intercept" :: Nil        => CliResult.Help(Some("Missing intercept tool"))
            case "intercept" :: tool :: rest =>
                tool match
                    case "maven" | "mvn" =>
                        CliResult.Run(InterceptTool.Maven, Chunk.from(rest))
                    case "sbt" =>
                        CliResult.Run(InterceptTool.Sbt, Chunk.from(rest))
                    case "gradle" =>
                        CliResult.Run(InterceptTool.Gradle, Chunk.from(rest))
                    case other =>
                        CliResult.Help(Some(s"Unsupported intercept tool: $other"))
            case "shim" :: "generate" :: rest =>
                parseShimGenerate(rest)
            case "shim" :: Nil =>
                CliResult.Help(Some("Missing shim subcommand. Use: shim generate"))
            case "shim" :: sub :: _ =>
                CliResult.Help(Some(s"Unknown shim subcommand: $sub"))
            case command :: _ => CliResult.Help(Some(s"Unsupported command: $command"))

    private def parseShimGenerate(args: List[String]): CliResult =
        parseShimOpts(args, ShimGenerateOptions.default)

    private def parseShimOpts(args: List[String], opts: ShimGenerateOptions): CliResult =
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
                                CliResult.Help(Some(s"Unsupported tool: $other. Use maven, gradle, sbt, or all"))
            case ("-t" | "--tool") :: Nil =>
                CliResult.Help(Some("Missing value for --tool"))
            case ("-w" | "--wrapper") :: rest =>
                parseShimOpts(rest, opts.copy(wrapper = true))
            case ("-o" | "--output-dir") :: dir :: rest =>
                parseShimOpts(rest, opts.copy(outputDir = Paths.get(dir)))
            case ("-o" | "--output-dir") :: Nil =>
                CliResult.Help(Some("Missing value for --output-dir"))
            case "--version" :: v :: rest =>
                parseShimOpts(rest, opts.copy(version = v))
            case "--version" :: Nil =>
                CliResult.Help(Some("Missing value for --version"))
            case unknown :: _ =>
                CliResult.Help(Some(s"Unknown shim generate option: $unknown"))
